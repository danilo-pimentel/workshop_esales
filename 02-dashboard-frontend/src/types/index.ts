export interface Produto {
  id: number
  nome: string
  descricao: string
  preco: number
  estoque: number
  categoria: string
  created_at: string
}

export interface Cliente {
  id: number
  nome: string
  email: string
  telefone: string
  created_at: string
}

export interface ItemPedido {
  id: number
  pedido_id: number
  produto_id: number
  quantidade: number
  preco_unitario: number
}

export interface Pedido {
  id: number
  cliente_id: number
  status: string
  total: number
  created_at: string
  itens?: ItemPedido[]
  cliente?: Cliente
}

export interface DashboardResumo {
  total_vendas: number
  total_pedidos: number
  ticket_medio: number
}

export interface PaginatedResponse<T> {
  data: T[]
  page: number
  limit: number
  total: number
  totalPages: number
}

export interface Column<T> {
  key: string
  label: string
  render?: (value: unknown, row: T) => React.ReactNode
  sortable?: boolean
}
