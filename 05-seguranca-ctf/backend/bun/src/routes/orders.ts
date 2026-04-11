/**
 * Order routes: listing, detail, create, apply coupon
 */

import Elysia from "elysia";
import { db } from "../db";
import { requireAuth, TokenPayload } from "../middleware/auth";
import { logRequest, setCurrentSql } from "../middleware/logger";

interface Order {
  id:         number;
  user_id:    number;
  total:      number;
  status:     string;
  created_at: string;
  user_name?: string;
  user_email?: string;
}

export const orderRoutes = new Elysia({ prefix: "/api/orders" })

  // ------------------------------------------------------------------
  // GET /api/orders
  // ------------------------------------------------------------------
  .get("/", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    let sqlQuery: string;
    let orders: Order[];

    if (tokenUser.role === "admin") {
      sqlQuery = "SELECT * FROM orders ORDER BY id DESC";
      setCurrentSql(sqlQuery);
      orders = db.query(sqlQuery).all() as Order[];
    } else {
      sqlQuery = `SELECT * FROM orders WHERE user_id = ${tokenUser.sub} ORDER BY id DESC`;
      setCurrentSql(sqlQuery);
      orders = db.query("SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC")
        .all(tokenUser.sub) as Order[];
    }

    logRequest({
      method: "GET", path: "/api/orders",
      queryParams: "", body: "", statusCode: 200,
      sqlQuery, responsePreview: `${orders.length} orders`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return { orders, count: orders.length };
  })

  // ------------------------------------------------------------------
  // GET /api/orders/:id
  // ------------------------------------------------------------------
  .get("/:id", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;

    const { id } = ctx.params as { id: string };
    const sqlQuery = `SELECT o.*, u.nome as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ${id}`;
    setCurrentSql(sqlQuery);

    const order = db.query(
      "SELECT o.*, u.nome as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?"
    ).get(id) as Order | undefined;

    logRequest({
      method: "GET", path: `/api/orders/${id}`,
      queryParams: "", body: "", statusCode: order ? 200 : 404,
      sqlQuery, responsePreview: order ? `order ${order.id}` : "not found",
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    if (!order) {
      ctx.set.status = 404;
      return { error: "Not Found", message: "Pedido nao encontrado" };
    }

    return order;
  })

  // ------------------------------------------------------------------
  // POST /api/orders
  // ------------------------------------------------------------------
  .post("/", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;
    const tokenUser = authResult as TokenPayload;

    let body: { items?: { product_id: number; quantity: number; price: number }[]; total?: number } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { items = [], total = 0 } = body;

    if (!items.length || total <= 0) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Pedido deve conter itens e total valido" };
    }

    db.run(
      "INSERT INTO orders (user_id, total, status) VALUES (?, ?, 'pendente')",
      [tokenUser.sub, total]
    );

    const newOrder = db.query("SELECT * FROM orders WHERE rowid = last_insert_rowid()").get() as Order;

    logRequest({
      method: "POST", path: "/api/orders",
      queryParams: "", body: JSON.stringify(body), statusCode: 201,
      sqlQuery: `INSERT INTO orders (user_id, total, status) VALUES (${tokenUser.sub}, ${total}, 'pendente')`,
      responsePreview: `order ${newOrder.id} created`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    ctx.set.status = 201;
    return { message: "Pedido criado com sucesso", order: newOrder };
  })

  // ------------------------------------------------------------------
  // POST /api/orders/:id/apply-coupon
  // ------------------------------------------------------------------
  .post("/:id/apply-coupon", async (ctx) => {
    const authResult = await requireAuth(ctx as Parameters<typeof requireAuth>[0]);
    if (authResult instanceof Response) return authResult;

    const { id } = ctx.params as { id: string };

    let body: { code?: string } = {};
    try {
      body = (ctx as any).body ?? (await ctx.request.clone().json());
    } catch {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "JSON invalido" };
    }

    const { code = "" } = body;

    if (!code) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Codigo do cupom obrigatorio" };
    }

    const coupon = db.query(
      "SELECT * FROM coupons WHERE code = ? AND active = 1"
    ).get(code) as { id: number; code: string; discount: number; max_uses: number; uses: number } | undefined;

    if (!coupon) {
      ctx.set.status = 404;
      return { error: "Not Found", message: "Cupom nao encontrado" };
    }

    // Check if coupon has remaining uses
    if (coupon.uses >= coupon.max_uses) {
      ctx.set.status = 400;
      return { error: "Bad Request", message: "Cupom esgotado" };
    }

    // Simulate processing delay for coupon validation
    await new Promise(resolve => setTimeout(resolve, 100));

    // Increment coupon usage
    db.run("UPDATE coupons SET uses = uses + 1 WHERE id = ?", [coupon.id]);

    const order = db.query("SELECT * FROM orders WHERE id = ?").get(id) as Order | undefined;
    if (!order) {
      ctx.set.status = 404;
      return { error: "Not Found", message: "Pedido nao encontrado" };
    }

    const discount = Math.round(order.total * (coupon.discount / 100) * 100) / 100;
    const newTotal = Math.round((order.total - discount) * 100) / 100;

    db.run("UPDATE orders SET total = ? WHERE id = ?", [newTotal, id]);

    logRequest({
      method: "POST", path: `/api/orders/${id}/apply-coupon`,
      queryParams: "", body: JSON.stringify(body), statusCode: 200,
      sqlQuery: `UPDATE coupons SET uses = uses + 1 WHERE id = ${coupon.id}; UPDATE orders SET total = ${newTotal} WHERE id = ${id}`,
      responsePreview: `coupon ${code} applied, discount ${discount}`,
      ip: ctx.request.headers.get("x-forwarded-for") ?? "unknown",
    });

    return {
      message: "Cupom aplicado com sucesso",
      discount,
      original_total: order.total,
      new_total: newTotal,
      coupon_code: code,
      coupon_uses: coupon.uses + 1,
      coupon_max: coupon.max_uses,
    };
  });
