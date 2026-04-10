import { useEffect, useState } from 'react';
import { getOrders } from '../api/client';

interface Order {
  id: number;
  user_id: number;
  total: number;
  status: string;
  created_at: string;
}

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);

  useEffect(() => {
    getOrders()
      .then((data) => setOrders(data.orders as Order[]))
      .catch(() => {});
  }, []);

  const statusColors: Record<string, string> = {
    pendente: 'bg-yellow-100 text-yellow-800',
    processando: 'bg-blue-100 text-blue-800',
    enviado: 'bg-purple-100 text-purple-800',
    entregue: 'bg-green-100 text-green-800',
    cancelado: 'bg-red-100 text-red-800',
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Meus Pedidos</h1>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 text-gray-600">
              <th className="text-left px-4 py-3 font-medium">#</th>
              <th className="text-left px-4 py-3 font-medium">Total</th>
              <th className="text-left px-4 py-3 font-medium">Status</th>
              <th className="text-left px-4 py-3 font-medium">Data</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id} className="border-t border-gray-100 hover:bg-gray-50">
                <td className="px-4 py-3 font-mono">{order.id}</td>
                <td className="px-4 py-3">R$ {order.total.toFixed(2)}</td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColors[order.status] || 'bg-gray-100'}`}>
                    {order.status}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-500">{order.created_at}</td>
              </tr>
            ))}
            {orders.length === 0 && (
              <tr><td colSpan={4} className="px-4 py-8 text-center text-gray-400">Nenhum pedido encontrado</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
