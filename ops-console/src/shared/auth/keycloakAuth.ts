export type AuthSession = {
  accessToken: string
  refreshToken?: string
  idToken?: string
  expiresAt: number
  refreshExpiresAt?: number
  username?: string
  roles: string[]
}

type TokenResponse = {
  access_token: string
  refresh_token?: string
  id_token?: string
  expires_in: number
  refresh_expires_in?: number
}

type PkceState = {
  state: string
  verifier: string
  redirectUri: string
}

const issuer = trimTrailingSlash(
  import.meta.env.VITE_BAZARFLOW_AUTH_ISSUER ?? 'http://localhost:8081/realms/bazarflow',
)
const clientId = import.meta.env.VITE_BAZARFLOW_AUTH_CLIENT_ID ?? 'bazarflow-ops-console'
const sessionStorageKey = 'bazarflow.auth.session'
const pkceStorageKey = 'bazarflow.auth.pkce'
const expirySkewMillis = 30_000

export function getStoredSession(): AuthSession | null {
  const stored = window.sessionStorage.getItem(sessionStorageKey)
  if (!stored) {
    return null
  }

  try {
    return JSON.parse(stored) as AuthSession
  } catch {
    window.sessionStorage.removeItem(sessionStorageKey)
    return null
  }
}

export async function getValidAccessToken(): Promise<string | null> {
  const session = getStoredSession()
  if (!session) {
    return null
  }

  if (session.expiresAt > Date.now() + expirySkewMillis) {
    return session.accessToken
  }

  if (!session.refreshToken || isRefreshExpired(session)) {
    clearSession()
    return null
  }

  return refreshAccessToken(session.refreshToken)
}

export async function completeLoginFromRedirect(): Promise<AuthSession | null> {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const returnedState = params.get('state')
  const error = params.get('error')

  if (error) {
    clearPendingLogin()
    stripLoginParams()
    throw new Error(error)
  }

  if (!code) {
    return getStoredSession()
  }

  const pendingLogin = readPendingLogin()
  clearPendingLogin()
  stripLoginParams()

  if (!pendingLogin || pendingLogin.state !== returnedState) {
    throw new Error('Invalid sign-in state')
  }

  const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: clientId,
      code,
      code_verifier: pendingLogin.verifier,
      redirect_uri: pendingLogin.redirectUri,
    }),
  })

  if (!response.ok) {
    throw new Error('Token exchange failed')
  }

  return storeTokenResponse((await response.json()) as TokenResponse)
}

export async function startLogin() {
  const verifier = randomBase64Url(32)
  const challenge = await codeChallenge(verifier)
  const state = randomBase64Url(24)
  const redirectUri = currentRedirectUri()

  const pendingLogin: PkceState = { state, verifier, redirectUri }
  window.sessionStorage.setItem(pkceStorageKey, JSON.stringify(pendingLogin))

  const authorizeUrl = new URL(`${issuer}/protocol/openid-connect/auth`)
  authorizeUrl.searchParams.set('client_id', clientId)
  authorizeUrl.searchParams.set('response_type', 'code')
  authorizeUrl.searchParams.set('scope', 'openid profile')
  authorizeUrl.searchParams.set('redirect_uri', redirectUri)
  authorizeUrl.searchParams.set('state', state)
  authorizeUrl.searchParams.set('code_challenge', challenge)
  authorizeUrl.searchParams.set('code_challenge_method', 'S256')

  window.location.assign(authorizeUrl.toString())
}

export function signOut() {
  const session = getStoredSession()
  clearSession()

  const logoutUrl = new URL(`${issuer}/protocol/openid-connect/logout`)
  logoutUrl.searchParams.set('client_id', clientId)
  logoutUrl.searchParams.set('post_logout_redirect_uri', currentRedirectUri())
  if (session?.idToken) {
    logoutUrl.searchParams.set('id_token_hint', session.idToken)
  }
  window.location.assign(logoutUrl.toString())
}

function storeTokenResponse(token: TokenResponse): AuthSession {
  const now = Date.now()
  const payload = decodeJwtPayload(token.access_token)
  const session: AuthSession = {
    accessToken: token.access_token,
    refreshToken: token.refresh_token,
    idToken: token.id_token,
    expiresAt: now + token.expires_in * 1000,
    refreshExpiresAt: token.refresh_expires_in !== undefined ? now + token.refresh_expires_in * 1000 : undefined,
    username: stringClaim(payload, 'preferred_username') ?? stringClaim(payload, 'name'),
    roles: rolesClaim(payload),
  }

  window.sessionStorage.setItem(sessionStorageKey, JSON.stringify(session))
  return session
}

async function refreshAccessToken(refreshToken: string) {
  const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: clientId,
      refresh_token: refreshToken,
    }),
  })

  if (!response.ok) {
    clearSession()
    return null
  }

  return storeTokenResponse((await response.json()) as TokenResponse).accessToken
}

function readPendingLogin(): PkceState | null {
  const stored = window.sessionStorage.getItem(pkceStorageKey)
  if (!stored) {
    return null
  }

  try {
    return JSON.parse(stored) as PkceState
  } catch {
    return null
  }
}

function clearPendingLogin() {
  window.sessionStorage.removeItem(pkceStorageKey)
}

function clearSession() {
  window.sessionStorage.removeItem(sessionStorageKey)
}

function isRefreshExpired(session: AuthSession) {
  return session.refreshExpiresAt !== undefined && session.refreshExpiresAt <= Date.now() + expirySkewMillis
}

function stripLoginParams() {
  window.history.replaceState({}, document.title, currentRedirectUri())
}

function currentRedirectUri() {
  return `${window.location.origin}${window.location.pathname}`
}

async function codeChallenge(verifier: string) {
  const data = new TextEncoder().encode(verifier)
  const hash = await window.crypto.subtle.digest('SHA-256', data)
  return base64Url(new Uint8Array(hash))
}

function randomBase64Url(size: number) {
  const bytes = new Uint8Array(size)
  window.crypto.getRandomValues(bytes)
  return base64Url(bytes)
}

function base64Url(bytes: Uint8Array) {
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }

  return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  const payload = token.split('.')[1]
  if (!payload) {
    return {}
  }

  try {
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=')
    return JSON.parse(window.atob(padded)) as Record<string, unknown>
  } catch {
    return {}
  }
}

function stringClaim(claims: Record<string, unknown>, key: string) {
  const value = claims[key]
  return typeof value === 'string' && value.length > 0 ? value : undefined
}

function rolesClaim(claims: Record<string, unknown>) {
  const realmAccess = claims.realm_access
  if (!isRecord(realmAccess) || !Array.isArray(realmAccess.roles)) {
    return []
  }

  return realmAccess.roles.filter((role): role is string => typeof role === 'string')
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function trimTrailingSlash(value: string) {
  return value.replace(/\/$/, '')
}
