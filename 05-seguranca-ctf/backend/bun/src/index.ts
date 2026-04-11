/**
 * SecureShop — E-commerce REST API
 *
 * Port: 4000
 * Monitor: http://localhost:4000/monitor
 */

import { Elysia } from "elysia";
import { readFileSync } from "fs";
import { join } from "path";

// Initialize DB (creates tables + seeds data on first run)
import "./db";

import { authRoutes }    from "./routes/auth";
import { userRoutes }    from "./routes/users";
import { productRoutes } from "./routes/products";
import { orderRoutes }   from "./routes/orders";
import { adminRoutes }   from "./routes/admin";
import { logRoutes }     from "./routes/logs";
import { exportRoutes }  from "./routes/export";

// ---------------------------------------------------------------------------
// App
// ---------------------------------------------------------------------------

const app = new Elysia()

  // Global CORS
  .onRequest((ctx) => {
    ctx.set.headers["Access-Control-Allow-Origin"]  = "*";
    ctx.set.headers["Access-Control-Allow-Methods"] = "GET,POST,PUT,DELETE,OPTIONS";
    ctx.set.headers["Access-Control-Allow-Headers"] = "Content-Type,Authorization,X-Role";
  })

  .options("/*", (ctx) => {
    ctx.set.status = 204;
    return "";
  })

  // Health check
  .get("/", () => ({
    service:  "SecureShop API",
    version:  "1.0.0",
    status:   "running",
    endpoints: {
      auth:     "/api/auth/login  /api/auth/register  /api/auth/forgot-password",
      users:    "/api/users/me  /api/users/:id",
      products: "/api/products  /api/products/search?q=  /api/products/:id/reviews",
      orders:   "/api/orders  /api/orders/:id  /api/orders/:id/apply-coupon",
      export:   "/api/export/:format",
      admin:    "/api/admin/users",
      logs:     "/api/logs",
      monitor:  "/monitor",
    },
  }))

  // Monitor dashboard
  .get("/monitor", (ctx) => {
    try {
      const html = readFileSync(
        join(import.meta.dir, "monitor", "dashboard.html"),
        "utf-8"
      );
      ctx.set.headers["Content-Type"] = "text/html; charset=utf-8";
      return new Response(html, {
        headers: { "Content-Type": "text/html; charset=utf-8" },
      });
    } catch {
      return new Response("<h1>Monitor nao encontrado</h1>", {
        status: 404,
        headers: { "Content-Type": "text/html" },
      });
    }
  })

  // Route groups
  .use(authRoutes)
  .use(userRoutes)
  .use(productRoutes)
  .use(orderRoutes)
  .use(adminRoutes)
  .use(logRoutes)
  .use(exportRoutes)

  // Global error handler
  .onError(({ error, set }) => {
    const err = error as Error;
    set.status = 500;
    return {
      error:   "Internal Server Error",
      message: err.message,
      stack:   (err.stack ?? "").split("\n").slice(0, 8),
    };
  })

  .listen(4000);

console.log("SecureShop API running on http://localhost:4000");

export type App = typeof app;
