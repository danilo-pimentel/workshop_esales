import { useState, FormEvent } from 'react';
import { searchProducts } from '../api/client';

interface SearchResult {
  id?: number;
  nome?: string;
  descricao?: string;
  preco?: number | string;
  categoria?: string;
}

export default function ProductSearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    setSearched(true);
    try {
      const data = await searchProducts(query);
      setResults(Array.isArray(data) ? (data as SearchResult[]) : [data as SearchResult]);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Erro na busca.';
      setError(msg);
      setResults([]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar produtos..."
          className="flex-1 border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        <button
          type="submit"
          disabled={loading}
          className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold px-6 py-2.5 rounded-lg transition-colors"
        >
          {loading ? '...' : 'Buscar'}
        </button>
      </form>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {results.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {results.map((item, i) => (
            <div key={i} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow">
              <div className="w-full h-40 bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center text-5xl select-none">
                📦
              </div>
              <div className="p-4">
                <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded uppercase tracking-wide">
                  {item.categoria ?? 'Geral'}
                </span>
                <h3 className="font-semibold text-gray-900 mt-2 truncate">{item.nome ?? '—'}</h3>
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{item.descricao ?? ''}</p>
                <div className="mt-3">
                  <span className="text-blue-700 font-bold text-lg">
                    {typeof item.preco === 'number'
                      ? item.preco.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
                      : String(item.preco ?? '')}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {searched && !loading && results.length === 0 && !error && (
        <p className="text-gray-500 text-center py-12">Nenhum produto encontrado.</p>
      )}
    </div>
  );
}
