/**
 * Export routes: download data in various formats
 */

import Elysia from "elysia";
import { readFileSync } from "fs";
import { join } from "path";
import { requireAuth, TokenPayload } from "../middleware/auth";
import { logRequest } from "../middleware/logger";

export const exportRoutes = new Elysia({ prefix: "/api" })

  // ------------------------------------------------------------------
  // GET /api/export/:format
  // ------------------------------------------------------------------
  .get("/export/:format", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;

    const { format } = ctx.params as { format: string };

    // Load the requested template file
    const templatePath = join(import.meta.dir, "..", "..", "templates", format);

    let template: string;
    try {
      template = readFileSync(templatePath, "utf-8");
    } catch {
      ctx.set.status = 404;
      return { error: "Not Found", message: `Template '${format}' nao encontrado` };
    }

    logRequest({
      method: "GET", path: `/api/export/${format}`,
      queryParams: "", body: "", statusCode: 200,
      sqlQuery: "", responsePreview: `Template ${format} loaded (${template.length} bytes)`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { format, template, message: "Template carregado" };
  });
