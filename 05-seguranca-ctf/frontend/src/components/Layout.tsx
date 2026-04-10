import { Outlet, Link, useNavigate } from 'react-router-dom';
import { logout } from '../api/client';

interface LayoutProps {
  user: { id: number; nome: string; email: string; role: string } | null;
  setDevToolsOpen: (open: boolean) => void;
}

export default function Layout({ user, setDevToolsOpen }: LayoutProps) {
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    window.dispatchEvent(new Event('secureshop:authchange'));
    navigate('/login');
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-blue-700 text-white shadow-md">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-xl font-bold tracking-tight hover:text-blue-200 transition-colors">
            SecureShop
          </Link>

          <nav className="flex items-center gap-6 text-sm font-medium">
            <Link to="/" className="hover:text-blue-200 transition-colors">Inicio</Link>
            <Link to="/products" className="hover:text-blue-200 transition-colors">Produtos</Link>
            {user && (
              <>
                <Link to="/orders" className="hover:text-blue-200 transition-colors">Pedidos</Link>
                <Link to="/checkout" className="hover:text-blue-200 transition-colors">Carrinho</Link>
              </>
            )}

            {user ? (
              <>
                <Link to={`/profile/${user.id}`} className="hover:text-blue-200 transition-colors">{user.nome}</Link>
                {user.role === 'admin' && (
                  <span className="bg-yellow-400 text-yellow-900 text-xs font-bold px-2 py-0.5 rounded">ADMIN</span>
                )}
                <button
                  onClick={handleLogout}
                  className="bg-blue-800 hover:bg-blue-900 px-3 py-1.5 rounded transition-colors"
                >
                  Sair
                </button>
              </>
            ) : (
              <Link to="/login" className="bg-white text-blue-700 hover:bg-blue-50 px-4 py-1.5 rounded font-semibold transition-colors">
                Entrar
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="bg-gray-800 text-gray-400 text-xs text-center py-4 mt-8">
        <p>
          SecureShop &copy; 2024{' '}
          <button
            onClick={() => setDevToolsOpen(true)}
            className="opacity-0 hover:opacity-100 transition-opacity text-gray-600 cursor-default select-none"
            title="DevTools"
            tabIndex={-1}
          >
            [dev]
          </button>
        </p>
      </footer>
    </div>
  );
}
