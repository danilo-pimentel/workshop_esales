import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getStoredUser } from '../api/client';

interface CartItem {
  product_id: number;
  nome: string;
  preco: number;
  quantity: number;
}

function getCart(): CartItem[] {
  try {
    return JSON.parse(localStorage.getItem('secureshop_cart') || '[]');
  } catch {
    return [];
  }
}

function saveCart(cart: CartItem[]) {
  localStorage.setItem('secureshop_cart', JSON.stringify(cart));
  window.dispatchEvent(new Event('secureshop:cartchange'));
}

export function addToCart(product: { id: number; nome: string; preco: number }) {
  const cart = getCart();
  const existing = cart.find((c) => c.product_id === product.id);
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({ product_id: product.id, nome: product.nome, preco: product.preco, quantity: 1 });
  }
  saveCart(cart);
}

export default function Checkout() {
  const user = getStoredUser();
  const [cart, setCart] = useState<CartItem[]>(getCart());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  useEffect(() => {
    function onCartChange() { setCart(getCart()); }
    window.addEventListener('secureshop:cartchange', onCartChange);
    return () => window.removeEventListener('secureshop:cartchange', onCartChange);
  }, []);

  const total = cart.reduce((sum, item) => sum + item.preco * item.quantity, 0);

  function updateQuantity(productId: number, qty: number) {
    const updated = cart.map((c) =>
      c.product_id === productId ? { ...c, quantity: Math.max(1, qty) } : c
    );
    saveCart(updated);
    setCart(updated);
  }

  function removeItem(productId: number) {
    const updated = cart.filter((c) => c.product_id !== productId);
    saveCart(updated);
    setCart(updated);
  }

  async function handleCheckout() {
    if (!user || cart.length === 0) return;
    setSubmitting(true);
    setResult(null);

    try {
      const token = localStorage.getItem('secureshop_token');
      // Send items with prices and the calculated total to the server
      const response = await fetch('http://localhost:4000/api/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          items: cart.map((c) => ({
            product_id: c.product_id,
            quantity: c.quantity,
            price: c.preco,
          })),
          total: Math.round(total * 100) / 100,
        }),
      });

      const data = await response.json();
      if (response.ok) {
        setResult(`Pedido #${data.order?.id} criado com sucesso! Total: R$ ${data.order?.total?.toFixed(2)}`);
        saveCart([]);
        setCart([]);
      } else {
        setResult(`Erro: ${data.message || response.statusText}`);
      }
    } catch (err) {
      setResult(`Erro: ${err instanceof Error ? err.message : 'Falha na requisicao'}`);
    } finally {
      setSubmitting(false);
    }
  }

  if (!user) {
    return (
      <div className="max-w-3xl mx-auto py-10 px-4">
        <p className="text-gray-500"><Link to="/login" className="text-blue-600 hover:underline">Faca login</Link> para finalizar sua compra.</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Checkout</h1>

      {cart.length === 0 && !result ? (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8 text-center">
          <p className="text-gray-400 mb-4">Seu carrinho esta vazio</p>
          <Link to="/products" className="text-blue-600 hover:underline text-sm">Ver produtos</Link>
        </div>
      ) : (
        <>
          {cart.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-6">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 text-gray-600">
                    <th className="text-left px-4 py-3 font-medium">Produto</th>
                    <th className="text-center px-4 py-3 font-medium">Qtd</th>
                    <th className="text-right px-4 py-3 font-medium">Preco</th>
                    <th className="text-right px-4 py-3 font-medium">Subtotal</th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {cart.map((item) => (
                    <tr key={item.product_id} className="border-t border-gray-100">
                      <td className="px-4 py-3">{item.nome}</td>
                      <td className="px-4 py-3 text-center">
                        <input
                          type="number"
                          min={1}
                          value={item.quantity}
                          onChange={(e) => updateQuantity(item.product_id, parseInt(e.target.value) || 1)}
                          className="w-16 text-center border border-gray-300 rounded px-2 py-1 text-sm"
                        />
                      </td>
                      <td className="px-4 py-3 text-right">R$ {item.preco.toFixed(2)}</td>
                      <td className="px-4 py-3 text-right font-medium">R$ {(item.preco * item.quantity).toFixed(2)}</td>
                      <td className="px-4 py-3 text-right">
                        <button onClick={() => removeItem(item.product_id)} className="text-red-500 hover:text-red-700 text-xs">Remover</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="border-t-2 border-gray-200 bg-gray-50">
                    <td colSpan={3} className="px-4 py-3 text-right font-bold text-gray-700">Total:</td>
                    <td className="px-4 py-3 text-right font-bold text-green-600 text-lg">R$ {total.toFixed(2)}</td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}

          {cart.length > 0 && (
            <button
              onClick={handleCheckout}
              disabled={submitting}
              className="w-full bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white font-bold py-3 rounded-lg transition-colors text-lg"
            >
              {submitting ? 'Processando...' : `Finalizar Compra — R$ ${total.toFixed(2)}`}
            </button>
          )}

          {result && (
            <div className={`mt-4 p-4 rounded-lg text-sm ${result.startsWith('Erro') ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-green-50 text-green-700 border border-green-200'}`}>
              {result}
            </div>
          )}
        </>
      )}
    </div>
  );
}
