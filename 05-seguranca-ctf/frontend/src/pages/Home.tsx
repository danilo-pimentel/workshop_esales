import { Link } from 'react-router-dom';

export default function Home() {
  return (
    <div className="max-w-5xl mx-auto px-4 py-12">
      {/* Hero */}
      <section className="text-center mb-14">
        <h1 className="text-4xl font-extrabold text-gray-900 mb-4">
          Bem-vindo à <span className="text-blue-600">SecureShop</span>
        </h1>
        <p className="text-lg text-gray-500 max-w-xl mx-auto mb-8">
          Sua loja online de confiança. Encontre os melhores produtos com os melhores preços.
        </p>
        <div className="flex justify-center gap-4">
          <Link
            to="/products"
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-8 py-3 rounded-xl transition-colors shadow"
          >
            Ver Produtos
          </Link>
          <Link
            to="/login"
            className="border border-gray-300 hover:border-blue-400 text-gray-700 hover:text-blue-600 font-semibold px-8 py-3 rounded-xl transition-colors"
          >
            Entrar
          </Link>
        </div>
      </section>

      {/* Feature cards */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-14">
        {[
          {
            icon: '🚚',
            title: 'Entrega Rápida',
            desc: 'Receba seus pedidos em até 3 dias úteis em todo o Brasil.',
          },
          {
            icon: '🔒',
            title: 'Compra Segura',
            desc: 'Pagamentos 100% protegidos. Seus dados estão em boas mãos.',
          },
          {
            icon: '⭐',
            title: 'Qualidade Garantida',
            desc: 'Mais de 10.000 produtos avaliados pelos nossos clientes.',
          },
        ].map(({ icon, title, desc }) => (
          <div
            key={title}
            className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 flex flex-col items-center text-center gap-3"
          >
            <span className="text-5xl">{icon}</span>
            <h3 className="text-base font-bold text-gray-800">{title}</h3>
            <p className="text-sm text-gray-500">{desc}</p>
          </div>
        ))}
      </section>

      {/* CTA banner */}
      <section className="bg-gradient-to-r from-blue-600 to-blue-800 rounded-2xl p-8 text-white text-center">
        <h2 className="text-2xl font-bold mb-2">Novidades da semana</h2>
        <p className="text-blue-200 mb-6">
          Confira as últimas ofertas e não perca as promoções exclusivas.
        </p>
        <Link
          to="/products"
          className="bg-white text-blue-700 hover:bg-blue-50 font-bold px-8 py-3 rounded-xl transition-colors shadow"
        >
          Ver Promoções
        </Link>
      </section>
    </div>
  );
}
