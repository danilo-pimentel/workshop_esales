import Elysia from "elysia";
import { db } from "../db";

interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string | null;
  created_at: string;
}

interface Pedido {
  id: number;
  cliente_id: number;
  status: string;
  total: number;
  created_at: string;
}

export const clientesRoutes = new Elysia({ prefix: "/api/clientes" })
  .get("/", () => {
    const clientes = db.query<Cliente, []>("SELECT * FROM clientes ORDER BY nome").all();
    return clientes;
  })

  .get("/:id", ({ params, set }) => {
    const id = Number(params.id);

    const cliente = db.query<Cliente, [number]>(
      "SELECT * FROM clientes WHERE id = ?"
    ).get(id);

    if (!cliente) {
      set.status = 404;
      return { error: "Cliente não encontrado" };
    }

    const pedidos = db.query<Pedido, [number]>(
      "SELECT * FROM pedidos WHERE cliente_id = ? ORDER BY created_at DESC"
    ).all(id);

    const totalGasto = pedidos.reduce((acc, p) => acc + p.total, 0);

    return {
      ...cliente,
      pedidos,
      total_pedidos: pedidos.length,
      total_gasto: Number(totalGasto.toFixed(2)),
    };
  });
