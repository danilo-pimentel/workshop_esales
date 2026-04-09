import { useCallback } from 'react'
import { api } from '../api/client'
import { useApi } from '../hooks/useApi'
import { DashboardResumo } from '../types'
import StatsCard from '../components/StatsCard'
import SalesChart from '../components/SalesChart'

export default function Dashboard() {
  const fetchResumo = useCallback(() => api.get<DashboardResumo>('/api/dashboard/resumo'), [])
  const { data: resumo, error } = useApi(fetchResumo)

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

  if (error) {
    return (
      <div className="rounded-xl bg-red-50 border border-red-200 px-6 py-5 text-sm text-red-700">
        <p className="font-semibold">Erro ao carregar dados</p>
        <p className="mt-1 text-red-500">{error}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Visão Geral</h2>
        <p className="text-sm text-gray-500 mt-0.5">Resumo das métricas de desempenho</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4" data-testid="stats-cards">
        <StatsCard
          title="Total de Vendas"
          value={resumo ? formatCurrency(resumo.total_vendas) : '—'}
          subtitle="Acumulado do período"
          icon={
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
              <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" />
            </svg>
          }
          trend={{ value: 12.5, label: 'vs. mês anterior' }}
          color="green"
        />
        <StatsCard
          title="Total de Pedidos"
          value={resumo ? resumo.total_pedidos.toLocaleString('pt-BR') : '—'}
          subtitle="Pedidos no período"
          icon={
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
              <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2" />
              <path d="M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          }
          trend={{ value: 8.2, label: 'vs. mês anterior' }}
          color="blue"
        />
        <StatsCard
          title="Ticket Médio"
          value={resumo ? formatCurrency(resumo.ticket_medio) : '—'}
          subtitle="Por pedido"
          icon={
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
              <path d="M9 14l6-6M15 9v.01M9 15v.01M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-3m6 3l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-3m0-1v1m0 0l6 3" />
            </svg>
          }
          trend={{ value: -2.1, label: 'vs. mês anterior' }}
          color="purple"
        />
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-base font-semibold text-gray-900">Desempenho de Vendas</h3>
            <p className="text-sm text-gray-400 mt-0.5">Histórico mensal acumulado</p>
          </div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
              <span className="h-1.5 w-1.5 rounded-full bg-blue-500" />
              Ao vivo
            </span>
          </div>
        </div>
        <div data-testid="sales-chart">
          <SalesChart data={resumo} />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="card">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Acesso Rápido</h3>
          <div className="space-y-2">
            {[
              { label: 'Novo produto', href: '/produtos', color: 'bg-blue-50 text-blue-700' },
              { label: 'Ver pedidos pendentes', href: '/pedidos', color: 'bg-yellow-50 text-yellow-700' },
              { label: 'Lista de clientes', href: '/clientes', color: 'bg-emerald-50 text-emerald-700' },
            ].map(link => (
              <a
                key={link.href}
                href={link.href}
                className={`flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium ${link.color} hover:opacity-80 transition-opacity`}
              >
                {link.label}
                <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                </svg>
              </a>
            ))}
          </div>
        </div>

        <div className="card">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Status do Sistema</h3>
          <div className="space-y-3">
            {[
              { label: 'API Backend', status: 'online', latency: '12ms' },
              { label: 'Banco de Dados', status: 'online', latency: '4ms' },
              { label: 'Cache', status: 'degraded', latency: '230ms' },
            ].map(item => (
              <div key={item.label} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <span className={`h-2 w-2 rounded-full ${item.status === 'online' ? 'bg-emerald-400' : 'bg-yellow-400'}`} />
                  <span className="text-gray-700">{item.label}</span>
                </div>
                <span className="text-gray-400 font-mono text-xs">{item.latency}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
