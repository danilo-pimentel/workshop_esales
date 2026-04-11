/**
 * User routes: GET /api/users/me, GET /api/users/:id, PUT /api/users/:id
 */

import Elysia from "elysia";
import { db } from "../db";
import { requireAuth, TokenPayload } from "../middleware/auth";
import { logRequest, setCurrentSql } from "../middleware/logger";

export const userRoutes = new Elysia({ prefix: "/api/users" })

  // ------------------------------------------------------------------
  // GET /api/users/me  — current user's own profile (safe reference)
  // ------------------------------------------------------------------
  .get("/me", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    const sqlQuery = `SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ${tokenUser.sub}`;
    setCurrentSql(sqlQuery);

    const user = db.query(
      "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?"
    ).get(tokenUser.sub);

    logRequest({
      method: "GET", path: "/api/users/me",
      queryParams: "", body: "", statusCode: user ? 200 : 404,
      sqlQuery, responsePreview: user ? "found" : "Not found",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    if (!user) {
      ctx.set.status = 404;
      return { error: "Not Found", message: "Usuario nao encontrado" };
    }

    return user;
  })

  // ------------------------------------------------------------------
  // GET /api/users/:id  — user profile by ID
  // ------------------------------------------------------------------
  .get("/:id", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;

    const { id } = ctx.params as { id: string };
    const sqlQuery = `SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ${id}`;
    setCurrentSql(sqlQuery);

    let user: any;
    try {
      user = db.query(
        "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?"
      ).get(id);
    } catch (error: unknown) {
      const msg = (error as Error).message;
      logRequest({
        method: "GET", path: `/api/users/${id}`,
        queryParams: "", body: "", statusCode: 500,
        sqlQuery, responsePreview: msg,
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });
      ctx.set.status = 500;
      return { error: "Database error", message: msg };
    }

    logRequest({
      method: "GET", path: `/api/users/${id}`,
      queryParams: "", body: "", statusCode: user ? 200 : 404,
      sqlQuery, responsePreview: user ? `user ${id}` : "Not found",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    if (!user) {
      ctx.set.status = 404;
      return { error: "Not Found", message: "Usuario nao encontrado" };
    }

    return user;
  })

  // ------------------------------------------------------------------
  // PUT /api/users/:id  — update profile
  // ------------------------------------------------------------------
  .put("/:id", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    const { id } = ctx.params as { id: string };

    // Only allow users to update their own profile
    if (String(id) !== tokenUser.sub && tokenUser.role !== "admin") {
      ctx.set.status = 403;
      return { error: "Forbidden", message: "Acesso negado" };
    }

    let body: { nome?: string; email?: string; password?: string; telefone?: string; endereco?: string } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { nome, email, password, telefone, endereco } = body;

    try {
      db.run(
        `UPDATE users SET
          nome     = COALESCE(?, nome),
          email    = COALESCE(?, email),
          password = COALESCE(?, password),
          telefone = COALESCE(?, telefone),
          endereco = COALESCE(?, endereco)
        WHERE id = ?`,
        [nome ?? null, email ?? null, password ?? null, telefone ?? null, endereco ?? null, id]
      );
    } catch (error: unknown) {
      const msg = (error as Error).message;
      ctx.set.status = 500;
      return { error: "Database error", message: msg };
    }

    logRequest({
      method: "PUT", path: `/api/users/${id}`,
      queryParams: "", body: JSON.stringify(body), statusCode: 200,
      sqlQuery: `UPDATE users SET ... WHERE id = ${id}`,
      responsePreview: "Updated",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    const updated = db.query(
      "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?"
    ).get(id);
    return { message: "Usuario atualizado", user: updated };
  });
