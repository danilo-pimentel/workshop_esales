import { Elysia } from "elysia";
import { initDatabase } from "./db";
import { produtosRoutes } from "./routes/produtos";
import { pedidosRoutes } from "./routes/pedidos";
import { clientesRoutes } from "./routes/clientes";
import { dashboardRoutes } from "./routes/dashboard";

// Initialize database schema and seed data
initDatabase();

export const app = new Elysia()
  .use(produtosRoutes)
  .use(pedidosRoutes)
  .use(clientesRoutes)
  .use(dashboardRoutes)
  .get("/", () => ({
    name: "API de Pedidos",
    version: "1.0.0",
    endpoints: {
      produtos: "/api/produtos",
      pedidos: "/api/pedidos",
      clientes: "/api/clientes",
      dashboard: "/api/dashboard/resumo",
    },
  }))
  .onError(({ code, error, set }) => {
    if (code === "VALIDATION") {
      set.status = 400;
      return { error: "Dados inválidos", details: error.message };
    }
    if (code === "NOT_FOUND") {
      set.status = 404;
      return { error: "Rota não encontrada" };
    }
    set.status = 500;
    return { error: "Erro interno do servidor" };
  })
  .listen(3000);

console.log(`🛒 API de Pedidos rodando em http://localhost:${app.server?.port}`);

export type App = typeof app;
