// ─── Domain types ────────────────────────────────────────────────────────────

export interface User {
  id: number;
  nome: string;
  email: string;
  role: string;
  created_at?: string;
}

export interface Product {
  id: number;
  nome: string;
  descricao: string;
  preco: number;
  categoria: string;
  estoque: number;
  imagem_url?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// ─── Request logging ─────────────────────────────────────────────────────────

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface LoggedRequest {
  id: string;
  timestamp: Date;
  method: HttpMethod | string;
  url: string;
  requestHeaders: Record<string, string>;
  requestBody?: string;
  status?: number;
  statusText?: string;
  responseBody?: string;
  responseHeaders?: Record<string, string>;
  duration?: number;
  error?: string;
  findings: SuspiciousFinding[];
}

export interface SuspiciousFinding {
  type: 'sql' | 'credential' | 'error' | 'pii';
  label: string;
  excerpt: string;
}

// ─── Custom request builder ───────────────────────────────────────────────────

export interface HeaderPair {
  key: string;
  value: string;
}

export interface CustomRequestState {
  method: HttpMethod;
  url: string;
  headers: HeaderPair[];
  body: string;
}
