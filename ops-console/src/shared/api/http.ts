import { getValidAccessToken } from '../auth/keycloakAuth'

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  const accessToken = await getValidAccessToken()

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  return fetch(input, {
    ...init,
    headers,
  })
}
