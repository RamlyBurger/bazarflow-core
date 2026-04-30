import {
  AlertTriangle,
  Archive,
  ClipboardList,
  Gauge,
  PackageCheck,
  Truck,
} from 'lucide-react'

export const navItems = [
  { label: 'Command', icon: Gauge },
  { label: 'Orders', icon: ClipboardList },
  { label: 'Inventory', icon: Archive },
  { label: 'Dispatch', icon: Truck },
  { label: 'Audit', icon: PackageCheck },
]

export const kpis = [
  { label: 'Open orders', value: '128', delta: '+14 today', tone: 'neutral' },
  { label: 'Reserved units', value: '3,840', delta: '91% fill rate', tone: 'success' },
  { label: 'SLA risk', value: '7', delta: '3 high priority', tone: 'warning' },
  { label: 'Expiry risk lots', value: '18', delta: 'next 7 days', tone: 'danger' },
]

export const orderQueue = [
  { id: 'ORD-1048', outlet: 'Kedai Maju SS2', zone: 'PJ-02', state: 'Reserved', quantity: '42', due: '10:30' },
  { id: 'ORD-1049', outlet: 'Cafe Seri Bukit', zone: 'KL-05', state: 'Submitted', quantity: '18', due: '11:00' },
  { id: 'ORD-1050', outlet: 'Mini Mart Aman', zone: 'SA-01', state: 'Accepted', quantity: '64', due: '12:15' },
  { id: 'ORD-1051', outlet: 'Restoran Iqbal', zone: 'KL-03', state: 'SLA Risk', quantity: '27', due: '09:50' },
]

export const risks = [
  { label: 'Frozen burger patties', meta: 'Lot FZ-24-118 expires in 5 days', severity: 'high' },
  { label: 'Chilled sausage links', meta: 'Lot CH-24-031 requires dispatch today', severity: 'medium' },
  { label: 'Route KL-03', meta: 'Projected 22 minutes late', severity: 'high' },
]

export const timeline = [
  { time: '08:12', label: 'Batch received', meta: 'FZ-24-118, 600 cartons' },
  { time: '08:38', label: 'Order submitted', meta: 'ORD-1048 by sales' },
  { time: '08:39', label: 'Stock reserved', meta: 'FEFO selected 2 lots' },
  { time: '08:51', label: 'Dispatch planned', meta: 'Route PJ-02 wave A' },
]

export const throughput = [
  { day: 'Mon', orders: 88, delivered: 81 },
  { day: 'Tue', orders: 96, delivered: 90 },
  { day: 'Wed', orders: 104, delivered: 98 },
  { day: 'Thu', orders: 128, delivered: 117 },
  { day: 'Fri', orders: 112, delivered: 104 },
]

export const backlog = [
  { zone: 'PJ', jobs: 18 },
  { zone: 'KL', jobs: 24 },
  { zone: 'SA', jobs: 12 },
  { zone: 'CH', jobs: 9 },
]

export const riskIcon = AlertTriangle
