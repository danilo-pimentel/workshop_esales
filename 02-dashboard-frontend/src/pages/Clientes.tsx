import { useState, useCallback } from 'react'
import { api } from '../api/client'
import { useApi } from '../hooks/useApi'
import { Cliente, Pedido } from '../types'
import Modal from '../components/Modal'

export default function Clientes() {
  const [selectedClient, setSelectedClient] = useState<Cliente | null>(null)
  const [clientePedidos, setClientePedidos] = useState<Pedido[]>([])
  const [loadingPedidos, setLoadingPedidos] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')

  const fetchClientes = useCallback(() => api.get<Cliente[]>('/api/clientes'), [])
  const { data: clientes, error } = useApi(fetchClientes)

  const filtered = clientes
    ? clientes.filter(c =>
        c.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.email.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : []

  const handleOpenClient = async (cliente: Cliente) => {
    setSelectedClient(cliente)
    setLoadingPedidos(true)
    setClientePedidos([])
    try {
      const detail = await api.get<{ cliente: Cliente; pedidos: Pedido[] }>(`/api/clientes/${cliente.id}`)
      setClientePedidos(detail.pedidos ?? [])
    } catch {
      setClientePedidos([])
    } finally {
      setLoadingPedidos(false)
    }
  }

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

  const totalGasto = clientePedidos.reduce((acc, p) => acc + p.total, 0)

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Clientes</h2>
        <p className="text-sm text-gray-500 mt-0.5">
          {clientes ? `${clientes.length} cliente(s) cadastrado(s)` : 'Carregando...'}
        </p>
      </div>

      <div className="card p-4">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd" />
          </svg>
          <input
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder="Buscar por nome ou email..."
            className="input-field pl-9"
          />
        </div>
      </div>

      {error && (
        <div className="rounded-xl bg-red-50 border border-red-200 px-6 py-4 text-sm text-red-700">
          Erro ao carregar clientes: {error}
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4" data-testid="clients-grid">
        {filtered.map(cliente => (
          <button
            key={cliente.id}
            onClick={() => handleOpenClient(cliente)}
            className="card text-left group hover:shadow-md transition-shadow cursor-pointer"
          >
            <div className="flex items-start gap-3">
              <div className="h-10 w-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
                {cliente.nome.charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-gray-900 truncate group-hover:text-blue-600 transition-colors">
                  {cliente.nome}
                </p>
                <p className="text-xs text-gray-500 truncate mt-0.5">{cliente.email}</p>
                <p className="text-xs text-gray-400 mt-0.5">{cliente.telefone}</p>
              </div>
            </div>
            <div className="mt-3 pt-3 border-t border-gray-100 flex items-center justify-between">
              <span className="text-xs text-gray-400">
                Desde {new Date(cliente.created_at).toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' })}
              </span>
              <span className="text-xs text-blue-600 font-medium group-hover:underline">
                Ver histórico →
              </span>
            </div>
          </button>
        ))}

        {filtered.length === 0 && !error && (
          <div className="col-span-full py-12 text-center text-sm text-gray-400">
            Nenhum cliente encontrado.
          </div>
        )}
      </div>

      <Modal
        isOpen={selectedClient !== null}
        onClose={() => { setSelectedClient(null); setClientePedidos([]) }}
        title={selectedClient ? `Histórico — ${selectedClient.nome}` : ''}
        size="lg"
      >
        {selectedClient && (
          <div className="space-y-4">
            <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-xl">
              <div className="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white font-bold">
                {selectedClient.nome.charAt(0).toUpperCase()}
              </div>
              <div>
                <p className="font-semibold text-gray-900">{selectedClient.nome}</p>
                <p className="text-sm text-gray-500">{selectedClient.email}</p>
                <p className="text-sm text-gray-500">{selectedClient.telefone}</p>
              </div>
              <div className="ml-auto text-right">
                <p className="text-xs text-gray-400">Total gasto</p>
                <p className="text-lg font-bold text-gray-900">{formatCurrency(totalGasto)}</p>
              </div>
            </div>

            <div>
              <p className="text-sm font-semibold text-gray-700 mb-2">
                Pedidos ({clientePedidos.length})
              </p>
              {loadingPedidos ? (
                <p className="text-sm text-gray-400 py-4 text-center">Carregando pedidos...</p>
              ) : clientePedidos.length === 0 ? (
                <p className="text-sm text-gray-400 py-4 text-center">Sem pedidos registrados.</p>
              ) : (
                <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                  {clientePedidos.map(pedido => (
                    <div
                      key={pedido.id}
                      className="flex items-center justify-between px-4 py-3 bg-white rounded-lg border border-gray-100"
                    >
                      <div>
                        <p className="text-sm font-medium text-gray-900">Pedido #{pedido.id}</p>
                        <p className="text-xs text-gray-400">
                          {new Date(pedido.created_at).toLocaleDateString('pt-BR')}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-semibold text-gray-900">{formatCurrency(pedido.total)}</p>
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                          pedido.status === 'entregue' ? 'bg-emerald-100 text-emerald-700' :
                          pedido.status === 'cancelado' ? 'bg-red-100 text-red-700' :
                          'bg-blue-100 text-blue-700'
                        }`}>
                          {pedido.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
