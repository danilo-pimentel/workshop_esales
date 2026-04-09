import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

const pageTitles: Record<string, string> = {
  '/': 'Dashboard',
  '/produtos': 'Produtos',
  '/pedidos': 'Pedidos',
  '/clientes': 'Clientes',
}

export default function Layout() {
  const location = useLocation()
  const title = pageTitles[location.pathname] ?? 'eSales'

  return (
    <div className="min-h-screen bg-gray-50">
      <Sidebar />
      <div className="ml-64">
        <Header title={title} />
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
