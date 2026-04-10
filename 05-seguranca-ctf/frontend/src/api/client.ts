const BASE_URL = 'http://localhost:4000';

// ─── Auth helpers ─────────────────────────────────────────────────────────────

export function getToken(): string | null {
  return localStorage.getItem('secureshop_token');
}

export function setToken(token: string) {
  localStorage.setItem('secureshop_token', token);
}

export function clearToken() {
  localStorage.removeItem('secureshop_token');
  localStorage.removeItem('secureshop_user');
}

export function getStoredUser() {
  const raw = localStorage.getItem('secureshop_user');
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

// ─── Base fetch wrapper ───────────────────────────────────────────────────────

interface FetchOptions extends RequestInit {
  /** Extra headers merged on top of defaults */
  extraHeaders?: Record<string, string>;
  /** When true, the Authorization header is NOT included */
  anonymous?: boolean;
}

export async function apiFetch<T = unknown>(
  path: string,
  options: FetchOptions = {}
): Promise<T> {
  const { extraHeaders = {}, anonymous = false, ...rest } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...extraHeaders,
  };

  if (!anonymous) {
    const token = getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    headers,
  });

  if (!res.ok) {
    // Let the caller decide what to do with errors — still parse the body
    const body = await res.json().catch(() => ({ message: res.statusText }));
    const err = new Error((body as { message?: string }).message ?? res.statusText) as Error & { status: number; body: unknown };
    err.status = res.status;
    err.body = body;
    throw err;
  }

  return res.json() as Promise<T>;
}

// ─── Auth endpoints ───────────────────────────────────────────────────────────

export async function login(email: string, password: string) {
  const data = await apiFetch<{ token: string; user: { id: number; nome: string; email: string; role: string } }>(
    '/api/auth/login',
    {
      method: 'POST',
      body: JSON.stringify({ email, password }),
      anonymous: true,
    }
  );
  setToken(data.token);
  localStorage.setItem('secureshop_user', JSON.stringify(data.user));
  return data;
}

export async function logout() {
  clearToken();
}

// ─── Product endpoints ────────────────────────────────────────────────────────

export async function searchProducts(search: string) {
  const qs = search ? `?q=${encodeURIComponent(search)}` : '';
  const data = await apiFetch<{ results: unknown[]; count: number; query: string }>(`/api/products/search${qs}`);
  return data.results;
}

// ─── User endpoints ───────────────────────────────────────────────────────────

export async function getUserById(id: number | string) {
  return apiFetch<unknown>(`/api/users/${id}`);
}

export async function getMyProfile() {
  return apiFetch<unknown>('/api/users/me');
}

// ─── Order endpoints ─────────────────────────────────────────────────────────

export async function getOrders() {
  return apiFetch<{ orders: unknown[]; count: number }>('/api/orders');
}

export async function getOrderById(id: number | string) {
  return apiFetch<unknown>(`/api/orders/${id}`);
}

export async function applyCoupon(orderId: number | string, code: string) {
  return apiFetch<unknown>(`/api/orders/${orderId}/apply-coupon`, {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

// ─── Review endpoints ────────────────────────────────────────────────────────

export async function getProductReviews(productId: number | string) {
  return apiFetch<{ reviews: unknown[]; count: number }>(`/api/products/${productId}/reviews`, { anonymous: true });
}

export async function addReview(productId: number | string, text: string, rating: number) {
  return apiFetch<unknown>(`/api/products/${productId}/reviews`, {
    method: 'POST',
    body: JSON.stringify({ text, rating }),
  });
}

// ─── Auth extra ──────────────────────────────────────────────────────────────

export async function register(nome: string, email: string, password: string) {
  return apiFetch<unknown>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ nome, email, password }),
    anonymous: true,
  });
}

export async function forgotPassword(email: string) {
  return apiFetch<unknown>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
    anonymous: true,
  });
}

// ─── Export endpoints ────────────────────────────────────────────────────────

export async function exportData(format: string) {
  return apiFetch<unknown>(`/api/export/${encodeURIComponent(format)}`);
}

// ─── Product listing ─────────────────────────────────────────────────────────

export async function getProducts(page = 1, limit = 10) {
  return apiFetch<{ products: unknown[]; total: number; page: number; limit: number }>(`/api/products?page=${page}&limit=${limit}`, { anonymous: true });
}

export async function getProductById(id: number | string) {
  return apiFetch<unknown>(`/api/products/${id}`, { anonymous: true });
}
