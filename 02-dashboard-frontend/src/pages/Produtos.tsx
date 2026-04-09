import { useState, useCallback } from 'react'
import { api } from '../api/client'
import { useApi } from '../hooks/useApi'
import { Produto, PaginatedResponse } from '../types'
import DataTable from '../components/DataTable'
import Modal from '../components/Modal'

interface ProductFormData {
  nome: string
  descricao: string
  preco: string
  estoque: string
  categoria: string
}

const INITIAL_FORM: ProductFormData = {
  nome: '',
  descricao: '',
  preco: '',
  estoque: '',
  categoria: '',
}

export default function Produtos() {
  const [page, setPage] = useState(1)
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Produto | null>(null)
  const [formData, setFormData] = useState<ProductFormData>(INITIAL_FORM)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const fetchProdutos = useCallback(
    () => api.get<PaginatedResponse<Produto>>(`/api/produtos?busca=${search}&page=${page}&limit=10`),
    [search, page]
  )

  const { data, error, refetch } = useApi(fetchProdutos)

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setSearch(searchInput)
    setPage(1)
  }

  const handleEdit = (produto: Produto) => {
    setEditingProduct(produto)
    setFormData({
      nome: produto.nome,
      descricao: produto.descricao,
      preco: String(produto.preco),
      estoque: String(produto.estoque),
      categoria: produto.categoria,
    })
    setSaveError(null)
    setIsModalOpen(true)
  }

  const handleSave = async () => {
    if (!editingProduct) return
    setSaving(true)
    setSaveError(null)
    try {
      await api.put(`/api/produtos/${editingProduct.id}`, {
        nome: formData.nome,
        descricao: formData.descricao,
        preco: parseFloat(formData.preco),
        estoque: parseInt(formData.estoque, 10),
        categoria: formData.categoria,
      })
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Erro ao salvar')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (produto: Produto) => {
    if (!window.confirm(`Deseja excluir "${produto.nome}"?`)) return
    try {
      await api.delete(`/api/produtos/${produto.id}`)
      refetch()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Erro ao excluir')
    }
  }

  const handleFormChange = (field: keyof ProductFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const columns = [
    { key: 'id', label: 'ID', sortable: true },
    { key: 'nome', label: 'Nome', sortable: true },
    { key: 'categoria', label: 'Categoria', sortable: true },
    {
      key: 'preco',
      label: 'Preço',
      sortable: true,
      render: (value: unknown) =>
        new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value)),
    },
    {
      key: 'estoque',
      label: 'Estoque',
      sortable: true,
      render: (value: unknown) => {
        const qty = Number(value)
        return (
          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
            qty === 0 ? 'bg-red-100 text-red-700' :
            qty < 10 ? 'bg-yellow-100 text-yellow-700' :
            'bg-emerald-100 text-emerald-700'
          }`}>
            {qty} un.
          </span>
        )
      },
    },
    {
      key: 'created_at',
      label: 'Cadastro',
      sortable: false,
      render: (value: unknown) =>
        new Date(String(value)).toLocaleDateString('pt-BR'),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Produtos</h2>
          <p className="text-sm text-gray-500 mt-0.5">
            {data ? `${data.total} produto(s) encontrado(s)` : 'Carregando...'}
          </p>
        </div>
        <button
          onClick={() => {
            setEditingProduct(null)
            setFormData(INITIAL_FORM)
            setIsModalOpen(true)
          }}
          className="btn-primary"
        >
          <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd" />
          </svg>
          Novo Produto
        </button>
      </div>

      <div className="card p-4">
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="flex-1 relative">
            <svg className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd" />
            </svg>
            <input
              type="text"
              value={searchInput}
              onChange={e => setSearchInput(e.target.value)}
              placeholder="Buscar por nome ou categoria..."
              className="input-field pl-9"
              data-testid="search-input"
            />
          </div>
          <button type="submit" className="btn-primary">Buscar</button>
          {search && (
            <button
              type="button"
              onClick={() => { setSearch(''); setSearchInput(''); setPage(1) }}
              className="btn-secondary"
            >
              Limpar
            </button>
          )}
        </form>
      </div>

      {error && (
        <div className="rounded-xl bg-red-50 border border-red-200 px-6 py-4 text-sm text-red-700">
          Erro ao carregar produtos: {error}
        </div>
      )}

      <div className="card p-0 overflow-hidden" data-testid="products-table">
        <DataTable
          columns={columns as Parameters<typeof DataTable>[0]['columns']}
          data={(data?.data ?? []) as unknown as Record<string, unknown>[]}
          currentPage={data?.page ?? 1}
          totalPages={data?.totalPages ?? 1}
          onPageChange={setPage}
          emptyMessage="Nenhum produto encontrado."
          actions={(row) => (
            <div className="flex items-center justify-end gap-2">
              <button
                onClick={() => handleEdit(row as unknown as Produto)}
                className="p-1.5 rounded-lg text-blue-600 hover:bg-blue-50 transition-colors"
                title="Editar produto"
                data-testid="edit-product-btn"
              >
                <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
              </button>
              <button
                onClick={() => handleDelete(row as unknown as Produto)}
                className="p-1.5 rounded-lg text-red-500 hover:bg-red-50 transition-colors"
                title="Excluir produto"
              >
                <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
          )}
        />
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingProduct ? `Editar: ${editingProduct.nome}` : 'Novo Produto'}
        size="lg"
        footer={
          <>
            <button onClick={() => setIsModalOpen(false)} className="btn-secondary">
              Cancelar
            </button>
            <button
              onClick={handleSave}
              disabled={saving}
              className="btn-primary"
              data-testid="save-product-btn"
            >
              {saving ? 'Salvando...' : 'Salvar'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {saveError && (
            <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
              {saveError}
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
              <input
                type="text"
                value={formData.nome}
                onChange={e => handleFormChange('nome', e.target.value)}
                className="input-field"
                placeholder="Nome do produto"
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
              <textarea
                value={formData.descricao}
                onChange={e => handleFormChange('descricao', e.target.value)}
                rows={3}
                className="input-field resize-none"
                placeholder="Descrição do produto"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Preço (R$)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={formData.preco}
                onChange={e => handleFormChange('preco', e.target.value)}
                className="input-field"
                placeholder="0,00"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Estoque</label>
              <input
                type="number"
                min="0"
                value={formData.estoque}
                onChange={e => handleFormChange('estoque', e.target.value)}
                className="input-field"
                placeholder="0"
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Categoria</label>
              <input
                type="text"
                value={formData.categoria}
                onChange={e => handleFormChange('categoria', e.target.value)}
                className="input-field"
                placeholder="Ex: Eletrônicos, Vestuário..."
              />
            </div>
          </div>
        </div>
      </Modal>
    </div>
  )
}
