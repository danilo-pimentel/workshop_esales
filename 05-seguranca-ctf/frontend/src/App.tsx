import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Layout from './components/Layout';
import DevToolsPanel from './components/DevToolsPanel';
import Home from './pages/Home';
import Login from './pages/Login';
import Products from './pages/Products';
import ProductDetail from './pages/ProductDetail';
import Orders from './pages/Orders';
import Profile from './pages/Profile';
import Checkout from './pages/Checkout';
import { useRequestLogger } from './hooks/useRequestLogger';
import { getStoredUser } from './api/client';

export default function App() {
  useRequestLogger();

  const [devToolsOpen, setDevToolsOpen] = useState(false);
  const [user, setUser] = useState(getStoredUser());

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.ctrlKey && e.shiftKey && e.key === 'D') {
        e.preventDefault();
        setDevToolsOpen((prev) => !prev);
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  useEffect(() => {
    function onAuthChange() {
      setUser(getStoredUser());
    }
    window.addEventListener('secureshop:authchange', onAuthChange);
    return () => window.removeEventListener('secureshop:authchange', onAuthChange);
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout user={user} setDevToolsOpen={setDevToolsOpen} />}>
          <Route index element={<Home />} />
          <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
          <Route path="/products" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route
            path="/orders"
            element={user ? <Orders /> : <Navigate to="/login" replace />}
          />
          <Route
            path="/checkout"
            element={user ? <Checkout /> : <Navigate to="/login" replace />}
          />
          {/* /profile redirects to /profile/:id with the logged-in user's ID */}
          <Route
            path="/profile"
            element={user ? <Navigate to={`/profile/${user.id}`} replace /> : <Navigate to="/login" replace />}
          />
          <Route
            path="/profile/:id"
            element={user ? <Profile /> : <Navigate to="/login" replace />}
          />
        </Route>
      </Routes>

      <DevToolsPanel open={devToolsOpen} onClose={() => setDevToolsOpen(false)} />
    </BrowserRouter>
  );
}
