import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Bell, Command, RefreshCw, Search } from 'lucide-react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  navItems,
  riskIcon,
} from '../features/dashboard/dashboardData'
import { fetchPlatformStatus } from '../shared/api/platformStatus'
import { fallbackDashboard, fetchReportingDashboard } from '../shared/api/reportingDashboard'
import './App.css'

const RiskIcon = riskIcon

function App() {
  const { data: platformStatus, isFetching } = useQuery({
    queryKey: ['platform-status'],
    queryFn: fetchPlatformStatus,
  })
  const { data: dashboard = fallbackDashboard, isFetching: isDashboardFetching } = useQuery({
    queryKey: ['reporting-dashboard'],
    queryFn: fetchReportingDashboard,
  })

  const moduleRows = useMemo(
    () => platformStatus?.modules.slice(0, 6) ?? [],
    [platformStatus],
  )

  return (
    <main className="shell">
      <aside className="sidebar" aria-label="Module navigation">
        <div className="brand">
          <Command size={20} aria-hidden="true" />
          <span>BazarFlow</span>
        </div>
        <nav>
          {navItems.map((item) => (
            <button
              className={item.label === 'Command' ? 'nav-item active' : 'nav-item'}
              key={item.label}
              type="button"
              title={item.label}
            >
              <item.icon size={18} aria-hidden="true" />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div className="search">
            <Search size={17} aria-hidden="true" />
            <span>Search orders, SKUs, lots, outlets</span>
          </div>
          <div className="topbar-actions">
            <button type="button" className="icon-button" title="Refresh">
              <RefreshCw size={17} aria-hidden="true" />
            </button>
            <button type="button" className="icon-button" title="Notifications">
              <Bell size={17} aria-hidden="true" />
            </button>
            <span className="tenant">MY Frozen Distribution</span>
          </div>
        </header>

        <section className="title-row">
          <div>
            <p className="eyebrow">Operations command center</p>
            <h1>Cold-chain orders, stock, and dispatch risk</h1>
          </div>
          <div className="status-pill">
            <span className={isFetching || isDashboardFetching ? 'pulse fetching' : 'pulse'} />
            {platformStatus?.phase ?? 'active-development'}
          </div>
        </section>

        <section className="kpi-strip" aria-label="Operational metrics">
          {dashboard.kpis.map((kpi) => (
            <article className={`kpi ${kpi.tone}`} key={kpi.label}>
              <span>{kpi.label}</span>
              <strong>{kpi.value}</strong>
              <small>{kpi.delta}</small>
            </article>
          ))}
        </section>

        <section className="grid two-column">
          <article className="panel">
            <div className="panel-heading">
              <h2>Work queue</h2>
              <span>Next dispatch window</span>
            </div>
            <div className="queue">
              {dashboard.orderQueue.map((order) => (
                <div className="queue-row" key={order.id}>
                  <div>
                    <strong>{order.id}</strong>
                    <span>{order.outlet}</span>
                  </div>
                  <span>{order.zone}</span>
                  <span>{order.quantity} units</span>
                  <span className={order.state === 'SLA Risk' ? 'state risk' : 'state'}>
                    {order.state}
                  </span>
                  <time>{order.due}</time>
                </div>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel-heading">
              <h2>Risk watch</h2>
              <span>Expiry and SLA</span>
            </div>
            <div className="risk-list">
              {dashboard.risks.map((risk) => (
                <div className={`risk-row ${risk.severity}`} key={risk.label}>
                  <RiskIcon size={18} aria-hidden="true" />
                  <div>
                    <strong>{risk.label}</strong>
                    <span>{risk.meta}</span>
                  </div>
                </div>
              ))}
            </div>
          </article>
        </section>

        <section className="grid three-column">
          <article className="panel chart-panel">
            <div className="panel-heading">
              <h2>Order throughput</h2>
              <span>Submitted vs delivered</span>
            </div>
            <ResponsiveContainer width="100%" height={210}>
              <AreaChart data={dashboard.throughput} margin={{ left: -20, right: 8, top: 10, bottom: 0 }}>
                <CartesianGrid stroke="#e5e7eb" vertical={false} />
                <XAxis dataKey="day" tickLine={false} axisLine={false} />
                <YAxis tickLine={false} axisLine={false} />
                <Tooltip />
                <Area type="monotone" dataKey="orders" stroke="#2563eb" fill="#dbeafe" />
                <Area type="monotone" dataKey="delivered" stroke="#0f766e" fill="#ccfbf1" />
              </AreaChart>
            </ResponsiveContainer>
          </article>

          <article className="panel chart-panel">
            <div className="panel-heading">
              <h2>Dispatch backlog</h2>
              <span>Jobs by zone</span>
            </div>
            <ResponsiveContainer width="100%" height={210}>
              <BarChart data={dashboard.backlog} margin={{ left: -20, right: 8, top: 10, bottom: 0 }}>
                <CartesianGrid stroke="#e5e7eb" vertical={false} />
                <XAxis dataKey="zone" tickLine={false} axisLine={false} />
                <YAxis tickLine={false} axisLine={false} />
                <Tooltip />
                <Bar dataKey="jobs" fill="#f59e0b" radius={[5, 5, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </article>

          <article className="panel">
            <div className="panel-heading">
              <h2>Audit timeline</h2>
              <span>Latest events</span>
            </div>
            <ol className="timeline">
              {dashboard.timeline.map((event) => (
                <li key={`${event.time}-${event.label}`}>
                  <time>{event.time}</time>
                  <div>
                    <strong>{event.label}</strong>
                    <span>{event.meta}</span>
                  </div>
                </li>
              ))}
            </ol>
          </article>
        </section>

        <section className="panel module-panel">
          <div className="panel-heading">
            <h2>Module map</h2>
            <span>{platformStatus?.service ?? 'bazarflow-core'}</span>
          </div>
          <div className="module-grid">
            {moduleRows.map((module) => (
              <div className="module-row" key={module.name}>
                <strong>{module.name}</strong>
                <span>{module.responsibility}</span>
                <em>{module.status}</em>
              </div>
            ))}
          </div>
        </section>
      </section>
    </main>
  )
}

export default App
