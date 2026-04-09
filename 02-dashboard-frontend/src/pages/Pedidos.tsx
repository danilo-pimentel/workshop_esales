import { useState, useCallback } from 'react'
import { api } from '../api/client'
import { useApi } from '../hooks/useApi'
import { Pedido, PaginatedResponse } from '../types'
import DataTable from '../components/DataTable'

const STATUS_CONFIG: Record<string, { label: string; classes: string }> = {
  pendente: { label: 'Pendente', classes: 'bg-yellow-100 text-yellow-700' },
  processando: { label: 'Processando', classes: 'bg-blue-100 text-blue-700' },
  enviado: { label: 'Enviado', classes: 'bg-purple-100 text-purple-700' },
  entregue: { label: 'Entregue', classes: 'bg-emerald-100 text-emerald-700' },
  cancelado: { label: 'Cancelado', classes: 'bg-red-100 text-red-700' },
}

export default function Pedidos() {
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState('')
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [pedidoDetail, setPedidoDetail] = useState<Pedido | null>(null)
  const [loadingDetail, setLoadingDetail] = useState(false)

  const fetchPedidos = useCallback(
    () => api.get<PaginatedResponse<Pedido>>(`/api/pedidos?page=${page}&limit=10`),
    [page]
  )

  const { data, error } = useApi(fetchPedidos)

  const filteredData = statusFilter
    ? (data?.data ?? []).filter(p => p.status === statusFilter)
    : (data?.data ?? [])

  const handleExpand = async (pedido: Pedido) => {
    if (expandedId === pedido.id) {
      setExpandedId(null)
      setPedidoDetail(null)
      return
    }
    setExpandedId(pedido.id)
    setLoadingDetail(true)
    try {
      const detail = await api.get<Pedido>(`/api/pedidos/${pedido.id}`)
      setPedidoDetail(detail)
    } catch {
      setPedidoDetail(null)
    } finally {
      setLoadingDetail(false)
    }
  }

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

  const columns = [
    { key: 'id', label: 'Nº Pedido', sortable: true },
    { key: 'cliente_id', label: 'Cliente ID', sortable: true },
    {
      key: 'status',
      label: 'Status',
      sortable: true,
      render: (value: unknown) => {
        const cfg = STATUS_CONFIG[String(value)] ?? { label: String(value), classes: 'bg-gray-100 text-gray-700' }
        return (
          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${cfg.classes}`}>
            {cfg.label}
          </span>
        )
      },
    },
    {
      key: 'total',
      label: 'Total',
      sortable: true,
      render: (value: unknown) => formatCurrency(Number(value)),
    },
    {
      key: 'created_at',
      label: 'Data',
      sortable: true,
      render: (value: unknown) =>
        new Date(String(value)).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Pedidos</h2>
          <p className="text-sm text-gray-500 mt-0.5">
            {data ? `${data.total} pedido(s) no total` : 'Carregando...'}
          </p>
        </div>
      </div>

      <div className="card p-4">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-medium text-gray-600">Filtrar por status:</span>
          <button
            onClick={() => setStatusFilter('')}
            className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
              !statusFilter ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            Todos
          </button>
          {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
            <button
              key={key}
              onClick={() => setStatusFilter(key)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                statusFilter === key ? cfg.classes + ' ring-2 ring-offset-1 ring-current' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {cfg.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="rounded-xl bg-red-50 border border-red-200 px-6 py-4 text-sm text-red-700">
          Erro ao carregar pedidos: {error}
        </div>
      )}

      <div className="card p-0 overflow-hidden" data-testid="orders-table">
        <DataTable
          columns={columns as Parameters<typeof DataTable>[0]['columns']}
          data={filteredData as unknown as Record<string, unknown>[]}
          currentPage={data?.page ?? 1}
          totalPages={data?.totalPages ?? 1}
          onPageChange={setPage}
          emptyMessage="Nenhum pedido encontrado."
          actions={(row) => {
            const pedido = row as unknown as Pedido
            return (
              <button
                onClick={() => handleExpand(pedido)}
                className="px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
              >
                {expandedId === pedido.id ? 'Ocultar ▲' : 'Detalhes ▼'}
              </button>
            )
          }}
        />

        {expandedId !== null && (
          <div className="border-t border-gray-100 bg-gray-50 px-6 py-4">
            <p className="text-sm font-semibold text-gray-700 mb-3">
              Detalhes do Pedido #{expandedId}
            </p>
            {loadingDetail ? (
              <p className="text-sm text-gray-400">Carregando...</p>
            ) : pedidoDetail?.itens && pedidoDetail.itens.length > 0 ? (
              <div className="space-y-2">
                {pedidoDetail.itens.map(item => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between text-sm bg-white rounded-lg px-4 py-2.5 border border-gray-100"
                  >
                    <span className="text-gray-600">
                      Produto #{item.produto_id} — {item.quantidade}x
                    </span>
                    <span className="font-medium text-gray-900">
                      {formatCurrency(item.preco_unitario * item.quantidade)}
                    </span>
                  </div>
                ))}
                {pedidoDetail.cliente && (
                  <div className="mt-3 pt-3 border-t border-gray-200 text-sm text-gray-500">
                    Cliente: <span className="font-medium text-gray-700">{pedidoDetail.cliente.nome}</span>
                    {' '}&middot;{' '}
                    {pedidoDetail.cliente.email}
                  </div>
                )}
              </div>
            ) : (
              <p className="text-sm text-gray-400">Sem itens para exibir.</p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
