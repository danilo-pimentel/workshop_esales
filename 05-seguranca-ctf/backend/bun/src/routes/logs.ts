/**
 * Log routes: view and manage request logs
 */

import Elysia from "elysia";
import { db } from "../db";
import { requireAuth, TokenPayload } from "../middleware/auth";
import { logRequest } from "../middleware/logger";

interface RequestLog {
  id:               number;
  method:           string;
  path:             string;
  query_params:     string;
  body:             string;
  status_code:      number;
  sql_query:        string;
  response_preview: string;
  ip:               string;
  created_at:       string;
}

export const logRoutes = new Elysia({ prefix: "/api" })

  // ------------------------------------------------------------------
  // GET /api/logs
  // ------------------------------------------------------------------
  .get("/logs", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    if (tokenUser.role !== "admin") {
      ctx.set.status = 403;
      return { error: "Forbidden", message: "Acesso negado" };
    }

    const url   = new URL(ctx.request.url);
    const limit = parseInt(url.searchParams.get("limit") ?? "100", 10);

    const logs = db
      .query("SELECT * FROM request_logs ORDER BY id DESC LIMIT ?")
      .all(limit) as RequestLog[];

    logRequest({
      method: "GET", path: "/api/logs",
      queryParams: url.search, body: "", statusCode: 200,
      sqlQuery: `SELECT * FROM request_logs ORDER BY id DESC LIMIT ${limit}`,
      responsePreview: `${logs.length} log entries returned`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { logs, count: logs.length };
  })

  // ------------------------------------------------------------------
  // DELETE /api/logs
  // ------------------------------------------------------------------
  .delete("/logs", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    if (tokenUser.role !== "admin") {
      ctx.set.status = 403;
      return { error: "Forbidden", message: "Acesso negado" };
    }

    db.run("DELETE FROM request_logs");

    logRequest({
      method: "DELETE", path: "/api/logs",
      queryParams: "", body: "", statusCode: 200,
      sqlQuery: "DELETE FROM request_logs",
      responsePreview: "All logs cleared",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { message: "Todos os logs foram apagados" };
  });
