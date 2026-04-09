import Elysia, { t } from "elysia";
import { db } from "../db";

interface Pedido {
  id: number;
  cliente_id: number;
  status: string;
  total: number;
  created_at: string;
}

interface ItemPedido {
  id: number;
  pedido_id: number;
  produto_id: number;
  quantidade: number;
  preco_unitario: number;
}

interface PedidoDetalhe extends Pedido {
  cliente_nome: string;
  cliente_email: string;
  itens: (ItemPedido & { produto_nome: string })[];
}

interface CountResult {
  total: number;
}

interface ProdutoPreco {
  id: number;
  preco: number;
  estoque: number;
  nome: string;
}

const STATUS_VALIDOS = ["pendente", "processando", "enviado", "entregue", "cancelado"] as const;

export const pedidosRoutes = new Elysia({ prefix: "/api/pedidos" })
  .get("/", ({ query }) => {
    const page = Number(query.page ?? 1);
    const limit = Number(query.limit ?? 10);
    const offset = (page - 1) * limit;

    const countResult = db.query<CountResult, []>(
      "SELECT COUNT(*) as total FROM pedidos"
    ).get();

    const total = countResult?.total ?? 0;
    const totalPages = Math.floor(total / limit);

    const pedidos = db.query<Pedido & { cliente_nome: string }, [number, number]>(`
      SELECT p.*, c.nome as cliente_nome
      FROM pedidos p
      JOIN clientes c ON c.id = p.cliente_id
      ORDER BY p.id DESC
      LIMIT ? OFFSET ?
    `).all(limit, offset);

    return {
      data: pedidos,
      page,
      limit,
      total,
      totalPages,
    };
  }, {
    query: t.Object({
      page: t.Optional(t.String()),
      limit: t.Optional(t.String()),
      status: t.Optional(t.String()),
      data_inicio: t.Optional(t.String()),
      data_fim: t.Optional(t.String()),
    }),
  })

  .get("/:id", ({ params, set }) => {
    const id = Number(params.id);

    const pedido = db.query<Pedido & { cliente_nome: string; cliente_email: string; cliente_telefone: string | null }, [number]>(`
      SELECT p.*, c.nome as cliente_nome, c.email as cliente_email, c.telefone as cliente_telefone
      FROM pedidos p
      JOIN clientes c ON c.id = p.cliente_id
      WHERE p.id = ?
    `).get(id);

    if (!pedido) {
      set.status = 404;
      return { error: "Pedido não encontrado" };
    }

    const itens = db.query<ItemPedido & { produto_nome: string }, [number]>(`
      SELECT ip.*, pr.nome as produto_nome
      FROM itens_pedido ip
      JOIN produtos pr ON pr.id = ip.produto_id
      WHERE ip.pedido_id = ?
    `).all(id);

    return {
      ...pedido,
      itens,
    } as PedidoDetalhe & { cliente_telefone: string | null };
  })

  .post("/", ({ body, set }) => {
    const { cliente_id, itens } = body as {
      cliente_id: number;
      itens: { produto_id: number; quantidade: number }[];
    };

    // Validate client exists
    const cliente = db.query<{ id: number }, [number]>(
      "SELECT id FROM clientes WHERE id = ?"
    ).get(cliente_id);

    if (!cliente) {
      set.status = 400;
      return { error: "Cliente não encontrado" };
    }

    if (!itens || itens.length === 0) {
      set.status = 400;
      return { error: "Pedido deve ter pelo menos um item" };
    }

    // Validate all products exist and have stock
    const produtoIds = itens.map((i) => i.produto_id);
    const placeholders = produtoIds.map(() => "?").join(", ");
    const produtos = db.query<ProdutoPreco, number[]>(
      `SELECT id, preco, estoque, nome FROM produtos WHERE id IN (${placeholders})`
    ).all(...produtoIds);

    if (produtos.length !== produtoIds.length) {
      set.status = 400;
      return { error: "Um ou mais produtos não encontrados" };
    }

    const produtoMap = new Map(produtos.map((p) => [p.id, p]));

    for (const item of itens) {
      const produto = produtoMap.get(item.produto_id);
      if (!produto) {
        set.status = 400;
        return { error: `Produto ${item.produto_id} não encontrado` };
      }
      if (produto.estoque < item.quantidade) {
        set.status = 400;
        return { error: `Estoque insuficiente para o produto ${produto.nome}` };
      }
    }

    let total = 0;
    for (const item of itens) {
      const produto = produtoMap.get(item.produto_id)!;
      total += produto.preco;
    }

    // Insert pedido
    const pedidoResult = db.query<{ id: number }, [number, number]>(
      `INSERT INTO pedidos (cliente_id, status, total) VALUES (?, 'pendente', ?) RETURNING id`
    ).get(cliente_id, total);

    const pedidoId = pedidoResult!.id;

    // Insert items and decrement stock
    const insertItem = db.prepare(
      `INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)`
    );

    for (const item of itens) {
      const produto = produtoMap.get(item.produto_id)!;
      insertItem.run(pedidoId, item.produto_id, item.quantidade, produto.preco);

      db.run("UPDATE produtos SET estoque = estoque - ? WHERE id = ?", [
        item.quantidade,
        item.produto_id,
      ]);
    }

    const novoPedido = db.query<Pedido, [number]>(
      "SELECT * FROM pedidos WHERE id = ?"
    ).get(pedidoId);

    set.status = 201;
    return novoPedido;
  }, {
    body: t.Object({
      cliente_id: t.Number(),
      itens: t.Array(
        t.Object({
          produto_id: t.Number(),
          quantidade: t.Number(),
        })
      ),
    }),
  })

  .patch("/:id/status", ({ params, body, set }) => {
    const id = Number(params.id);
    const { status } = body as { status: string };

    const pedido = db.query<Pedido, [number]>(
      "SELECT * FROM pedidos WHERE id = ?"
    ).get(id);

    if (!pedido) {
      set.status = 404;
      return { error: "Pedido não encontrado" };
    }

    if (!STATUS_VALIDOS.includes(status as typeof STATUS_VALIDOS[number])) {
      set.status = 400;
      return {
        error: `Status inválido. Valores permitidos: ${STATUS_VALIDOS.join(", ")}`,
      };
    }

    db.run("UPDATE pedidos SET status = ? WHERE id = ?", [status, id]);

    const updated = db.query<Pedido, [number]>(
      "SELECT * FROM pedidos WHERE id = ?"
    ).get(id);

    return updated;
  }, {
    body: t.Object({
      status: t.String(),
    }),
  });
