import LoginForm from '../components/LoginForm';

export default function Login() {
  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="bg-white rounded-2xl shadow-lg border border-gray-200 p-8">
          <div className="text-center mb-7">
            <span className="text-5xl">🛒</span>
            <h1 className="text-2xl font-bold text-gray-900 mt-3">Entrar na SecureShop</h1>
            <p className="text-gray-500 text-sm mt-1">Use suas credenciais para continuar</p>
          </div>

          <LoginForm />
        </div>

        <p className="text-center text-xs text-gray-400 mt-4">
          Problemas para entrar? Contate o suporte.
        </p>
      </div>
    </div>
  );
}
