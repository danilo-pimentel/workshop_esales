import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getProductById, getProductReviews, addReview, getStoredUser } from '../api/client';
import { addToCart } from './Checkout';

interface Product {
  id: number;
  nome: string;
  descricao: string;
  preco: number;
  categoria: string;
}

interface Review {
  id: number;
  user_name: string;
  rating: number;
  text: string;
  created_at: string;
}

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewText, setReviewText] = useState('');
  const [reviewRating, setReviewRating] = useState(5);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const user = getStoredUser();

  useEffect(() => {
    if (!id) return;
    getProductById(id).then((p) => setProduct(p as Product)).catch(() => {});
    loadReviews();
  }, [id]);

  function loadReviews() {
    if (!id) return;
    getProductReviews(id).then((data) => setReviews((data as any).reviews ?? [])).catch(() => {});
  }

  async function handleSubmitReview(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !reviewText.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await addReview(id, reviewText, reviewRating);
      setReviewText('');
      setReviewRating(5);
      loadReviews();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao enviar review');
    } finally {
      setSubmitting(false);
    }
  }

  if (!product) {
    return (
      <div className="max-w-3xl mx-auto py-12 px-4">
        <p className="text-gray-500">Carregando produto...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto py-8 px-4">
      <Link to="/products" className="text-blue-600 hover:underline text-sm mb-4 inline-block">&larr; Voltar aos produtos</Link>

      {/* Product info */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{product.nome}</h1>
            <p className="text-sm text-gray-500 mt-1">{product.categoria}</p>
          </div>
          <span className="text-2xl font-bold text-green-600">
            R$ {product.preco.toFixed(2)}
          </span>
        </div>
        <p className="text-gray-700 mt-4">{product.descricao}</p>
        {user && (
          <button
            onClick={() => { addToCart(product); alert(`${product.nome} adicionado ao carrinho!`); }}
            className="mt-4 bg-green-600 hover:bg-green-700 text-white font-medium px-6 py-2 rounded-lg transition-colors"
          >
            Adicionar ao carrinho
          </button>
        )}
      </div>

      {/* Reviews section */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-lg font-bold text-gray-900 mb-4">
          Avaliacoes ({reviews.length})
        </h2>

        {reviews.length === 0 && (
          <p className="text-gray-400 text-sm mb-6">Nenhuma avaliacao ainda. Seja o primeiro!</p>
        )}

        {/* Reviews list */}
        <div className="space-y-4 mb-8">
          {reviews.map((review) => (
            <div key={review.id} className="border-b border-gray-100 pb-4">
              <div className="flex items-center justify-between">
                <span className="font-medium text-gray-800">{review.user_name}</span>
                <div className="flex items-center gap-1">
                  {Array.from({ length: 5 }, (_, i) => (
                    <span key={i} className={i < review.rating ? 'text-yellow-400' : 'text-gray-300'}>★</span>
                  ))}
                </div>
              </div>
              <div
                className="text-gray-600 text-sm mt-1"
                dangerouslySetInnerHTML={{ __html: review.text }}
              />
              <span className="text-xs text-gray-400 mt-1 block">{review.created_at}</span>
            </div>
          ))}
        </div>

        {/* Submit review form */}
        {user ? (
          <form onSubmit={handleSubmitReview} className="space-y-3">
            <h3 className="text-sm font-semibold text-gray-700">Escrever avaliacao</h3>
            {error && <p className="text-red-600 text-sm">{error}</p>}
            <div className="flex items-center gap-2">
              <label className="text-sm text-gray-600">Nota:</label>
              <select
                value={reviewRating}
                onChange={(e) => setReviewRating(Number(e.target.value))}
                className="border border-gray-300 rounded px-2 py-1 text-sm"
              >
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>{n} estrela{n > 1 ? 's' : ''}</option>
                ))}
              </select>
            </div>
            <textarea
              value={reviewText}
              onChange={(e) => setReviewText(e.target.value)}
              placeholder="Conte sua experiencia com este produto..."
              rows={3}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              type="submit"
              disabled={submitting || !reviewText.trim()}
              className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
            >
              {submitting ? 'Enviando...' : 'Enviar avaliacao'}
            </button>
          </form>
        ) : (
          <p className="text-sm text-gray-500">
            <Link to="/login" className="text-blue-600 hover:underline">Faca login</Link> para escrever uma avaliacao.
          </p>
        )}
      </div>
    </div>
  );
}
