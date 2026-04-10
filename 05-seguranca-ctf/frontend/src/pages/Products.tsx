import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ProductSearch from '../components/ProductSearch';
import { getProducts } from '../api/client';

interface Product {
  id: number;
  nome: string;
  descricao: string;
  preco: number;
  categoria: string;
}

export default function Products() {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const limit = 10;

  useEffect(() => {
    getProducts(page, limit)
      .then((data) => {
        setProducts(data.products as Product[]);
        setTotal(data.total);
      })
      .catch(() => {});
  }, [page]);

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold text-gray-900">Catalogo de Produtos</h1>
        <p className="text-gray-500 mt-1 text-sm">
          Pesquise por nome, categoria ou descricao.
        </p>
      </div>

      <ProductSearch />

      {/* Product listing */}
      <div className="mt-10">
        <h2 className="text-xl font-bold text-gray-800 mb-4">Todos os Produtos ({total})</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {products.map((p) => (
            <Link
              key={p.id}
              to={`/products/${p.id}`}
              className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 hover:shadow-md transition-shadow"
            >
              <h3 className="font-semibold text-gray-900">{p.nome}</h3>
              <p className="text-xs text-gray-500 mt-1">{p.categoria}</p>
              <p className="text-sm text-gray-600 mt-2 line-clamp-2">{p.descricao}</p>
              <p className="text-lg font-bold text-green-600 mt-3">R$ {p.preco.toFixed(2)}</p>
            </Link>
          ))}
        </div>

        {/* Pagination */}
        {total > limit && (
          <div className="flex justify-center gap-2 mt-6">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page <= 1}
              className="px-3 py-1 text-sm border rounded disabled:opacity-30"
            >
              Anterior
            </button>
            <span className="px-3 py-1 text-sm text-gray-600">
              Pagina {page} de {Math.ceil(total / limit)}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= Math.ceil(total / limit)}
              className="px-3 py-1 text-sm border rounded disabled:opacity-30"
            >
              Proxima
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
