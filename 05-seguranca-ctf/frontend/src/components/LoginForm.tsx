import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, forgotPassword } from '../api/client';

export default function LoginForm() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Forgot password state
  const [showForgot, setShowForgot] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotResult, setForgotResult] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      window.dispatchEvent(new Event('secureshop:authchange'));
      navigate('/');
    } catch (err) {
      const msg =
        err instanceof Error ? err.message : 'Erro ao realizar login. Tente novamente.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  async function handleForgotPassword(e: FormEvent) {
    e.preventDefault();
    if (!forgotEmail.trim()) return;
    setForgotLoading(true);
    setForgotResult(null);
    try {
      await forgotPassword(forgotEmail);
      setForgotResult("Se o email estiver cadastrado, voce recebera as instrucoes de recuperacao.");
    } catch {
      setForgotResult("Se o email estiver cadastrado, voce recebera as instrucoes de recuperacao.");
    } finally {
      setForgotLoading(false);
    }
  }

  return (
    <div className="space-y-5">
      {!showForgot ? (
        <form onSubmit={handleSubmit} className="space-y-5">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              E-mail
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="seu@email.com"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              Senha
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-2.5 rounded-lg transition-colors"
          >
            {loading ? 'Entrando...' : 'Entrar'}
          </button>

          <div className="flex items-center justify-between text-xs text-gray-500">
            <button
              type="button"
              onClick={() => setShowForgot(true)}
              className="text-blue-600 hover:underline"
            >
              Esqueci minha senha
            </button>
            <span>Ainda nao tem conta? Fale com o administrador.</span>
          </div>
        </form>
      ) : (
        <form onSubmit={handleForgotPassword} className="space-y-5">
          <h3 className="text-lg font-semibold text-gray-900">Recuperar senha</h3>
          <p className="text-sm text-gray-500">
            Informe seu e-mail cadastrado e enviaremos as instrucoes de recuperacao.
          </p>

          <div>
            <label htmlFor="forgot-email" className="block text-sm font-medium text-gray-700 mb-1">
              E-mail
            </label>
            <input
              id="forgot-email"
              type="email"
              value={forgotEmail}
              onChange={(e) => setForgotEmail(e.target.value)}
              required
              placeholder="seu@email.com"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
            />
          </div>

          <button
            type="submit"
            disabled={forgotLoading}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-2.5 rounded-lg transition-colors"
          >
            {forgotLoading ? 'Enviando...' : 'Enviar instrucoes'}
          </button>

          {forgotResult && (
            <pre className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-xs overflow-auto max-h-64 whitespace-pre-wrap">
              {forgotResult}
            </pre>
          )}

          <button
            type="button"
            onClick={() => { setShowForgot(false); setForgotResult(null); }}
            className="text-xs text-blue-600 hover:underline"
          >
            &larr; Voltar ao login
          </button>
        </form>
      )}
    </div>
  );
}
