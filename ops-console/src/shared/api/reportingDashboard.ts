import {
  backlog as fallbackBacklog,
  kpis as fallbackKpis,
  orderQueue as fallbackOrderQueue,
  risks as fallbackRisks,
  throughput as fallbackThroughput,
  timeline as fallbackTimeline,
} from '../../features/dashboard/dashboardData'

export type DashboardKpi = {
  label: string
  value: string
  delta: string
  tone: string
}

export type WorkQueueItem = {
  id: string
  outlet: string
  zone: string
  state: string
  quantity: string
  due: string
}

export type RiskItem = {
  label: string
  meta: string
  severity: string
}

export type ThroughputPoint = {
  day: string
  orders: number
  delivered: number
}

export type DispatchBacklogPoint = {
  zone: string
  jobs: number
  atRisk: number
}

export type TimelineItem = {
  time: string
  label: string
  meta: string
}

export type DashboardView = {
  kpis: DashboardKpi[]
  orderQueue: WorkQueueItem[]
  risks: RiskItem[]
  throughput: ThroughputPoint[]
  backlog: DispatchBacklogPoint[]
  timeline: TimelineItem[]
}

type ReportingDashboardResponse = {
  kpis: Array<{
    label: string
    value: string
    detail: string
    tone: string
  }>
  workQueue: Array<{
    orderNumber: string
    outletName: string
    deliveryZone: string
    requestedDeliveryDate: string
    orderStatus: string
    dispatchStatus: string
    totalQuantity: number | string
    slaAtRisk: boolean
    deliveryWindowEnd: string | null
  }>
  risks: RiskItem[]
  throughput: Array<{
    day: string
    submittedOrders: number
    deliveredOrders: number
  }>
  dispatchBacklog: Array<{
    deliveryZone: string
    openJobs: number
    atRiskJobs: number
  }>
  auditTimeline: Array<{
    sourceModule: string
    aggregateType: string
    eventType: string
    message: string
    occurredAt: string
  }>
}

export const fallbackDashboard: DashboardView = {
  kpis: fallbackKpis,
  orderQueue: fallbackOrderQueue,
  risks: fallbackRisks,
  throughput: fallbackThroughput,
  backlog: fallbackBacklog.map((item) => ({ ...item, atRisk: 0 })),
  timeline: fallbackTimeline,
}

export async function fetchReportingDashboard(): Promise<DashboardView> {
  try {
    const response = await fetch('/api/reporting/dashboard', {
      headers: {
        Accept: 'application/json',
      },
    })

    if (!response.ok) {
      return fallbackDashboard
    }

    const dashboard = (await response.json()) as ReportingDashboardResponse
    return mapDashboard(dashboard)
  } catch {
    return fallbackDashboard
  }
}

function mapDashboard(dashboard: ReportingDashboardResponse): DashboardView {
  return {
    kpis: dashboard.kpis.map((kpi) => ({
      label: kpi.label,
      value: kpi.value,
      delta: kpi.detail,
      tone: kpi.tone,
    })),
    orderQueue: dashboard.workQueue.map((item) => ({
      id: item.orderNumber,
      outlet: item.outletName,
      zone: item.deliveryZone,
      state: item.slaAtRisk ? 'SLA Risk' : formatStatus(preferredStatus(item)),
      quantity: formatQuantity(item.totalQuantity),
      due: item.deliveryWindowEnd?.slice(0, 5) ?? formatDate(item.requestedDeliveryDate),
    })),
    risks: dashboard.risks,
    throughput: dashboard.throughput.map((point) => ({
      day: formatDay(point.day),
      orders: point.submittedOrders,
      delivered: point.deliveredOrders,
    })),
    backlog: dashboard.dispatchBacklog.map((point) => ({
      zone: point.deliveryZone,
      jobs: point.openJobs,
      atRisk: point.atRiskJobs,
    })),
    timeline: dashboard.auditTimeline.map((event) => ({
      time: formatTime(event.occurredAt),
      label: formatStatus(event.eventType),
      meta: `${event.sourceModule} ${event.aggregateType.toLowerCase()}`,
    })),
  }
}

function preferredStatus(item: ReportingDashboardResponse['workQueue'][number]) {
  return item.dispatchStatus === 'UNPLANNED' ? item.orderStatus : item.dispatchStatus
}

function formatStatus(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function formatQuantity(value: number | string) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    return String(value)
  }
  return numericValue.toLocaleString(undefined, { maximumFractionDigits: 3 })
}

function formatDay(value: string) {
  return new Intl.DateTimeFormat(undefined, { weekday: 'short' }).format(new Date(`${value}T00:00:00Z`))
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(`${value}T00:00:00Z`))
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}
