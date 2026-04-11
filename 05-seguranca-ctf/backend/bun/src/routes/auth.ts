/**
 * Authentication routes: POST /api/auth/login, POST /api/auth/register
 */

import Elysia from "elysia";
import { readFileSync } from "fs";
import { join } from "path";
import { db } from "../db";
import { signToken } from "../middleware/auth";
import { logRequest, setCurrentSql } from "../middleware/logger";

// SMTP configuration — loaded from config file at startup
const smtpConfigPath = join(import.meta.dir, "..", "..", "config", "smtp.json");
let smtpConfig: { host: string; port: number; user: string; pass: string } | null = null;
try {
  smtpConfig = JSON.parse(readFileSync(smtpConfigPath, "utf-8"));
} catch {
  // Config file not found — email features will be unavailable
}

interface User {
  id:         number;
  nome:       string;
  email:      string;
  password:   string;
  role:       string;
  created_at: string;
}

export const authRoutes = new Elysia({ prefix: "/api/auth" })

  // ------------------------------------------------------------------
  // POST /api/auth/login
  // ------------------------------------------------------------------
  .post("/login", async (ctx) => {
    let sqlQuery   = "";
    let statusCode = 200;

    let body: { email?: string; password?: string } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      statusCode = 400;
      logRequest({
        method: "POST", path: "/api/auth/login", queryParams: "",
        body: "", statusCode, sqlQuery: "", responsePreview: "Invalid JSON",
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { email = "", password = "" } = body;

    // Log the full query for monitoring
    sqlQuery = `SELECT * FROM users WHERE email = '${email}' AND password = '${password}'`;
    setCurrentSql(sqlQuery);

    let user: User | undefined;
    try {
      user = db.query(
        `SELECT * FROM users WHERE email = ? AND password = '${password}'`
      ).get(email) as User | undefined;
    } catch (error: unknown) {
      statusCode = 500;
      const msg = (error as Error).message;
      logRequest({
        method: "POST", path: "/api/auth/login", queryParams: "",
        body: JSON.stringify(body), statusCode, sqlQuery,
        responsePreview: msg,
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });
      ctx.set.status = 500;
      return { error: "Database error", message: msg };
    }

    if (!user) {
      statusCode = 401;
      logRequest({
        method: "POST", path: "/api/auth/login", queryParams: "",
        body: JSON.stringify(body), statusCode, sqlQuery,
        responsePreview: "Invalid credentials",
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });
      ctx.set.status = 401;
      return { error: "Unauthorized", message: "Credenciais invalidas" };
    }

    const token = await signToken({
      sub:   String(user.id),
      email: user.email,
      role:  user.role,
    });

    logRequest({
      method: "POST", path: "/api/auth/login", queryParams: "",
      body: JSON.stringify(body), statusCode, sqlQuery,
      responsePreview: JSON.stringify({ id: user.id, email: user.email, role: user.role }),
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return {
      token,
      user: { id: user.id, nome: user.nome, email: user.email, role: user.role },
    };
  })

  // ------------------------------------------------------------------
  // POST /api/auth/register
  // ------------------------------------------------------------------
  .post("/register", async (ctx) => {
    let statusCode = 201;
    let sqlQuery   = "";

    let body: { nome?: string; email?: string; password?: string; role?: string } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { nome = "", email = "", password = "", role = "user" } = body;

    if (!nome || !email || !password) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Campos obrigatorios: nome, email, password" };
    }

    sqlQuery = `INSERT INTO users (nome, email, password, role) VALUES ('${nome}', '${email}', '${password}', '${role}')`;
    setCurrentSql(sqlQuery);

    try {
      db.run(
        "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, ?)",
        [nome, email, password, role]
      );
    } catch (error: unknown) {
      statusCode = 409;
      const msg = (error as Error).message;
      logRequest({
        method: "POST", path: "/api/auth/register", queryParams: "",
        body: JSON.stringify(body), statusCode, sqlQuery,
        responsePreview: msg,
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });
      ctx.set.status = 409;
      return { error: "Conflict", message: "Email ja cadastrado" };
    }

    const newUser = db
      .query("SELECT id, nome, email, role FROM users WHERE email = ?")
      .get(email) as { id: number; nome: string; email: string; role: string };

    logRequest({
      method: "POST", path: "/api/auth/register", queryParams: "",
      body: JSON.stringify(body), statusCode, sqlQuery,
      responsePreview: JSON.stringify(newUser),
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    ctx.set.status = 201;
    return { message: "Usuario criado com sucesso", user: newUser };
  })

  // ------------------------------------------------------------------
  // POST /api/auth/forgot-password
  // ------------------------------------------------------------------
  .post("/forgot-password", async (ctx) => {
    let body: { email?: string } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { email = "" } = body;

    if (!email) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Campo 'email' obrigatorio" };
    }

    // Prepare SMTP connection first to fail fast if misconfigured
    // (avoids leaking whether an email exists via response timing)
    try {
      const smtpConnection = `${smtpConfig!.host}:${smtpConfig!.port}`;
      const smtpAuth       = `${smtpConfig!.user}:${smtpConfig!.pass}`;

      const user = db.query("SELECT id, nome, email FROM users WHERE email = ?").get(email) as
        { id: number; nome: string; email: string } | undefined;

      if (user) {
        const resetToken = await signToken({
          sub:   String(user.id),
          email: user.email,
          role:  "reset",
        });
        const resetUrl = `http://localhost:5173/reset-password?token=${resetToken}`;
        console.log(`[SMTP] Sending reset email to ${user.email} via ${smtpConnection}`);
      }

      logRequest({
        method: "POST", path: "/api/auth/forgot-password", queryParams: "",
        body: JSON.stringify({ email }), statusCode: 200,
        sqlQuery: `SELECT id, nome, email FROM users WHERE email = '${email}'`,
        responsePreview: "Reset email sent",
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });

      return {
        message: "Se o email estiver cadastrado, voce recebera as instrucoes de recuperacao.",
      };
    } catch (err: unknown) {
      const error = err as Error;

      logRequest({
        method: "POST", path: "/api/auth/forgot-password", queryParams: "",
        body: JSON.stringify({ email }), statusCode: 500,
        sqlQuery: "", responsePreview: error.message,
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });

      ctx.set.status = 500;
      return {
        error:   "Internal Server Error",
        message: error.message,
        stack:   (error.stack ?? "").split("\n").slice(0, 8),
      };
    }
  });
