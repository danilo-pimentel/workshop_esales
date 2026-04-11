/**
 * JWT authentication middleware.
 */

import { jwtVerify, SignJWT } from "jose";

export const JWT_SECRET = "super-secret-jwt-key-2024";

const SECRET_BYTES = new TextEncoder().encode(JWT_SECRET);

export interface TokenPayload {
  sub:   string;
  email: string;
  role:  string;
  iat?:  number;
  exp?:  number;
}

/** Sign a new JWT for the given user (24 h expiry). */
export async function signToken(payload: Omit<TokenPayload, "iat" | "exp">): Promise<string> {
  return new SignJWT({ email: payload.email, role: payload.role })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(payload.sub)
    .setIssuedAt()
    .setExpirationTime("24h")
    .sign(SECRET_BYTES);
}

/** Verify a JWT and return its payload, or null if invalid. */
export async function verifyToken(token: string): Promise<TokenPayload | null> {
  try {
    const { payload } = await jwtVerify(token, SECRET_BYTES);
    return {
      sub:   payload.sub as string,
      email: payload.email as string,
      role:  payload.role as string,
    };
  } catch {
    return null;
  }
}

/**
 * Elysia-compatible auth guard.
 * Returns the token payload on success, or a 401 Response on failure.
 */
export async function requireAuth(
  ctx: { request: Request; set: { status: number }; headers: Record<string, string | undefined> }
): Promise<TokenPayload | Response> {
  const authHeader = ctx.request.headers.get("authorization") ?? "";
  const token      = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";

  if (!token) {
    ctx.set.status = 401;
    return Response.json({ error: "Unauthorized", message: "Token nao fornecido" }, { status: 401 });
  }

  const payload = await verifyToken(token);
  if (!payload) {
    ctx.set.status = 401;
    return Response.json({ error: "Unauthorized", message: "Token invalido ou expirado" }, { status: 401 });
  }

  return payload;
}
