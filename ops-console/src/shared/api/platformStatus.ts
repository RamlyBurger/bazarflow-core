import { apiFetch } from './http'

export type PlatformModule = {
  name: string
  responsibility: string
  status: string
}

export type PlatformStatus = {
  service: string
  phase: string
  checkedAt: string
  modules: PlatformModule[]
}

const fallbackStatus: PlatformStatus = {
  service: 'bazarflow-core',
  phase: 'active-development',
  checkedAt: new Date().toISOString(),
  modules: [
    { name: 'partner', responsibility: 'Retailers, outlets, zones, and credit status', status: 'implemented' },
    { name: 'catalog', responsibility: 'Products, SKUs, categories, and storage classes', status: 'implemented' },
    { name: 'inventory', responsibility: 'Lots, stock movements, reservations, and expiry risk', status: 'implemented' },
    { name: 'pricing', responsibility: 'Contract pricing, tiers, campaigns, and surcharges', status: 'implemented' },
    { name: 'ordering', responsibility: 'Order intake, idempotency, and state transitions', status: 'implemented' },
    { name: 'fulfillment', responsibility: 'Pick waves, dispatch jobs, delivery outcomes, and SLA risk', status: 'implemented' },
    { name: 'audit', responsibility: 'Append-only operational audit', status: 'implemented' },
    { name: 'reporting', responsibility: 'Operations dashboard read models', status: 'in-progress' },
  ],
}

export async function fetchPlatformStatus(): Promise<PlatformStatus> {
  try {
    const response = await apiFetch('/api/platform/status', {
      headers: {
        Accept: 'application/json',
      },
    })

    if (!response.ok) {
      return fallbackStatus
    }

    return response.json() as Promise<PlatformStatus>
  } catch {
    return fallbackStatus
  }
}
