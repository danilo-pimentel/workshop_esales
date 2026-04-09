import { useState, ReactNode } from 'react'
import { Column } from '../types'

interface DataTableProps<T extends Record<string, unknown>> {
  columns: Column<T>[]
  data: T[]
  keyField?: string
  emptyMessage?: string
  currentPage?: number
  totalPages?: number
  onPageChange?: (page: number) => void
  actions?: (row: T) => ReactNode
}

export default function DataTable<T extends Record<string, unknown>>({
  columns,
  data,
  keyField = 'id',
  emptyMessage = 'Nenhum registro encontrado.',
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  actions,
}: DataTableProps<T>) {
  const [sortKey, setSortKey] = useState<string>('')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc')

  const handleSort = (key: string) => {
    if (sortKey === key) {
      setSortDir(d => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir('asc')
    }
  }

  const getSortIcon = (key: string) => {
    if (sortKey !== key) {
      return (
        <svg className="h-4 w-4 text-gray-300" viewBox="0 0 20 20" fill="currentColor">
          <path d="M5 12l5-5 5 5H5z" />
        </svg>
      )
    }
    return sortDir === 'asc' ? (
      <svg className="h-4 w-4 text-blue-500" viewBox="0 0 20 20" fill="currentColor">
        <path d="M5 12l5-5 5 5H5z" />
      </svg>
    ) : (
      <svg className="h-4 w-4 text-blue-500" viewBox="0 0 20 20" fill="currentColor">
        <path d="M15 8l-5 5-5-5h10z" />
      </svg>
    )
  }

  return (
    <div className="w-full">
      <table className="min-w-full divide-y divide-gray-200" data-testid="data-table">
        <thead>
          <tr>
            {columns.map(col => (
              <th
                key={col.key}
                className={`table-header ${col.sortable ? 'cursor-pointer select-none hover:bg-gray-100' : ''}`}
                onClick={col.sortable ? () => handleSort(col.key) : undefined}
              >
                <div className="flex items-center gap-1.5">
                  {col.label}
                  {col.sortable && getSortIcon(col.key)}
                </div>
              </th>
            ))}
            {actions && (
              <th className="table-header text-right">Ações</th>
            )}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-100">
          {data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + (actions ? 1 : 0)}
                className="px-4 py-12 text-center text-sm text-gray-400"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row, idx) => (
              <tr
                key={String(row[keyField] ?? idx)}
                className="hover:bg-gray-50 transition-colors"
              >
                {columns.map(col => (
                  <td key={col.key} className="table-cell">
                    {col.render
                      ? col.render(row[col.key], row)
                      : String(row[col.key] ?? '—')}
                  </td>
                ))}
                {actions && (
                  <td className="table-cell text-right">
                    {actions(row)}
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>

      {totalPages > 1 && (
        <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
          <p className="text-sm text-gray-500">
            Página <span className="font-medium">{currentPage}</span> de{' '}
            <span className="font-medium">{totalPages}</span>
          </p>
          <div className="flex items-center gap-1">
            <button
              onClick={() => onPageChange?.(currentPage - 1)}
              disabled={currentPage <= 1}
              className="px-3 py-1.5 text-sm rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              ← Anterior
            </button>
            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const page = i + 1
              return (
                <button
                  key={page}
                  onClick={() => onPageChange?.(page)}
                  className={`w-8 h-8 text-sm rounded-lg transition-colors ${
                    page === currentPage
                      ? 'bg-blue-600 text-white font-medium'
                      : 'border border-gray-200 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  {page}
                </button>
              )
            })}
            <button
              onClick={() => onPageChange?.(currentPage + 1)}
              disabled={currentPage >= totalPages}
              className="px-3 py-1.5 text-sm rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Próxima →
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
