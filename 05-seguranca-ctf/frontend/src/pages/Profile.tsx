import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getUserById } from '../api/client';

interface UserData {
  id: number;
  nome: string;
  email: string;
  role: string;
  telefone?: string;
  cpf_last4?: string;
  endereco?: string;
  created_at?: string;
}

export default function Profile() {
  const { id } = useParams<{ id: string }>();
  const [user, setUser] = useState<UserData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    getUserById(id)
      .then((data) => setUser(data as UserData))
      .catch((err) => setError(err instanceof Error ? err.message : 'Erro ao carregar perfil'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10">
        <p className="text-gray-500">Carregando perfil...</p>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10">
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-4">
          {error || 'Usuario nao encontrado'}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Perfil</h1>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center text-3xl font-bold text-blue-600 select-none">
            {user.nome.charAt(0).toUpperCase()}
          </div>
          <div>
            <h2 className="text-xl font-bold text-gray-900">{user.nome}</h2>
            <p className="text-gray-500 text-sm">{user.email}</p>
            <span className={`inline-block mt-1 text-xs font-bold px-2 py-0.5 rounded ${
              user.role === 'admin' ? 'bg-yellow-100 text-yellow-800' : 'bg-gray-100 text-gray-600'
            }`}>
              {user.role}
            </span>
          </div>
        </div>

        {(user.telefone || user.cpf_last4 || user.endereco) && (
          <div className="border-t border-gray-100 pt-4 space-y-3">
            {user.telefone && (
              <div className="flex items-center gap-3">
                <span className="text-gray-400 text-sm w-24">Telefone</span>
                <span className="text-gray-800 text-sm">{user.telefone}</span>
              </div>
            )}
            {user.cpf_last4 && (
              <div className="flex items-center gap-3">
                <span className="text-gray-400 text-sm w-24">CPF</span>
                <span className="text-gray-800 text-sm">***.***.***-{user.cpf_last4}</span>
              </div>
            )}
            {user.endereco && (
              <div className="flex items-center gap-3">
                <span className="text-gray-400 text-sm w-24">Endereco</span>
                <span className="text-gray-800 text-sm">{user.endereco}</span>
              </div>
            )}
          </div>
        )}

        {user.created_at && (
          <p className="text-xs text-gray-400 mt-4 border-t border-gray-100 pt-3">
            Cadastrado em {user.created_at}
          </p>
        )}
      </div>
    </div>
  );
}
