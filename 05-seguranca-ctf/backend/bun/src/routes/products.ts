/**
 * Product routes: listing, search, detail, import, reviews
 */

import Elysia from "elysia";
import { db } from "../db";
import { requireAuth, TokenPayload } from "../middleware/auth";
import { logRequest, setCurrentSql } from "../middleware/logger";

function sanitizeInput(input: string): string {
  return input.replace(/[<>"&]/g, "");
}

export const productRoutes = new Elysia({ prefix: "/api/products" })

  // ------------------------------------------------------------------
  // GET /api/products
  // ------------------------------------------------------------------
  .get("/", (ctx) => {
    const url    = new URL(ctx.request.url);
    const page   = parseInt(url.searchParams.get("page") ?? "1", 10);
    const limit  = parseInt(url.searchParams.get("limit") ?? "10", 10);
    const offset = (page - 1) * limit;

    const sqlQuery = `SELECT * FROM products LIMIT ${limit} OFFSET ${offset}`;
    setCurrentSql(sqlQuery);

    const products = db.query("SELECT * FROM products LIMIT ? OFFSET ?").all(limit, offset);
    const total    = (db.query("SELECT COUNT(*) as c FROM products").get() as { c: number }).c;

    logRequest({
      method: "GET", path: "/api/products",
      queryParams: url.search, body: "", statusCode: 200,
      sqlQuery, responsePreview: `${(products as any[]).length} products`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { products, total, page, limit };
  })

  // ------------------------------------------------------------------
  // GET /api/products/search?q=<term>
  // ------------------------------------------------------------------
  .get("/search", (ctx) => {
    const url    = new URL(ctx.request.url);
    const rawSearch = url.searchParams.get("q") ?? "";

    const search = sanitizeInput(rawSearch);

    const sqlQuery =
      `SELECT id, nome, descricao, preco, categoria, created_at ` +
      `FROM products WHERE nome LIKE '%${search}%'`;

    setCurrentSql(sqlQuery);

    let results: unknown[];
    try {
      results = db.query(sqlQuery).all() as unknown[];
    } catch (error: unknown) {
      const msg = (error as Error).message;
      logRequest({
        method: "GET", path: "/api/products/search",
        queryParams: url.search, body: "", statusCode: 500,
        sqlQuery, responsePreview: msg,
        ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
      });
      ctx.set.status = 500;
      return { error: "Database error", message: msg };
    }

    logRequest({
      method: "GET", path: "/api/products/search",
      queryParams: url.search, body: "", statusCode: 200,
      sqlQuery, responsePreview: `${results.length} results`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { results, count: results.length, query: search };
  })

  // ------------------------------------------------------------------
  // GET /api/products/:id
  // ------------------------------------------------------------------
  .get("/:id", (ctx) => {
    const { id } = ctx.params as { id: string };

    // Avoid matching "import" as an :id route
    if (id === "import") return;
    // Avoid matching "search" as :id
    if (id === "search") return;

    const sqlQuery = `SELECT * FROM products WHERE id = ${id}`;
    setCurrentSql(sqlQuery);

    let product: unknown;
    try {
      product = db.query("SELECT * FROM products WHERE id = ?").get(id);
    } catch (error: unknown) {
      const msg = (error as Error).message;
      ctx.set.status = 500;
      return { error: "Database error", message: msg };
    }

    logRequest({
      method: "GET", path: `/api/products/${id}`,
      queryParams: "", body: "", statusCode: product ? 200 : 404,
      sqlQuery, responsePreview: product ? "found" : "not found",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    if (!product) {
      ctx.set.status = 404;
      return { error: "Not Found", message: "Produto nao encontrado" };
    }

    return product;
  })

  // ------------------------------------------------------------------
  // GET /api/products/:id/reviews
  // ------------------------------------------------------------------
  .get("/:id/reviews", (ctx) => {
    const { id } = ctx.params as { id: string };

    const reviews = db.query(
      "SELECT * FROM reviews WHERE product_id = ? ORDER BY created_at DESC"
    ).all(id);

    logRequest({
      method: "GET", path: `/api/products/${id}/reviews`,
      queryParams: "", body: "", statusCode: 200,
      sqlQuery: `SELECT * FROM reviews WHERE product_id = ${id}`,
      responsePreview: `${(reviews as any[]).length} reviews`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { reviews, count: (reviews as any[]).length };
  })

  // ------------------------------------------------------------------
  // POST /api/products/:id/reviews
  // ------------------------------------------------------------------
  .post("/:id/reviews", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    const { id } = ctx.params as { id: string };

    let body: { text?: string; rating?: number } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { text = "", rating = 5 } = body;

    if (!text) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Campo 'text' obrigatorio" };
    }

    if (rating < 1 || rating > 5) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Rating deve ser entre 1 e 5" };
    }

    const userRow = db.query("SELECT nome FROM users WHERE id = ?").get(tokenUser.sub) as { nome: string } | undefined;
    const userName = userRow?.nome ?? "Usuario";

    db.run(
      "INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)",
      [id, tokenUser.sub, userName, rating, text]
    );

    const review = db.query("SELECT * FROM reviews WHERE rowid = last_insert_rowid()").get();

    logRequest({
      method: "POST", path: `/api/products/${id}/reviews`,
      queryParams: "", body: JSON.stringify(body), statusCode: 201,
      sqlQuery: `INSERT INTO reviews (product_id, user_id, ...) VALUES (${id}, ${tokenUser.sub}, ...)`,
      responsePreview: "review created",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    ctx.set.status = 201;
    return { message: "Review adicionada", review };
  });
