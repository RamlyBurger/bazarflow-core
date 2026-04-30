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
  phase: 'phase-0-bootstrap',
  checkedAt: new Date().toISOString(),
  modules: [
    { name: 'partner', responsibility: 'Retailers, outlets, zones, and credit status', status: 'planned' },
    { name: 'catalog', responsibility: 'Products, SKUs, categories, and storage classes', status: 'planned' },
    { name: 'inventory', responsibility: 'Lots, stock movements, reservations, and expiry risk', status: 'planned' },
    { name: 'pricing', responsibility: 'Contract pricing, tiers, campaigns, and surcharges', status: 'planned' },
    { name: 'ordering', responsibility: 'Order intake, idempotency, and state transitions', status: 'planned' },
    { name: 'fulfillment', responsibility: 'Pick waves, dispatch jobs, and SLA risk', status: 'planned' },
    { name: 'audit', responsibility: 'Append-only operational audit and hash-chain checks', status: 'planned' },
    { name: 'reporting', responsibility: 'Operations dashboard read models', status: 'bootstrap' },
  ],
}

export async function fetchPlatformStatus(): Promise<PlatformStatus> {
  try {
    const response = await fetch('/api/platform/status', {
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
