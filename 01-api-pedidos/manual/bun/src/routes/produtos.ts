import Elysia, { t } from "elysia";
import { db } from "../db";

interface Produto {
  id: number;
  nome: string;
  descricao: string | null;
  preco: number;
  estoque: number;
  categoria: string | null;
  created_at: string;
}

interface CountResult {
  total: number;
}

export const produtosRoutes = new Elysia({ prefix: "/api/produtos" })
  .get("/", ({ query }) => {
    const busca = query.busca ?? "";
    const page = Number(query.page ?? 1);
    const limit = Number(query.limit ?? 10);
    const offset = (page - 1) * limit;

    let countQuery: string;
    let dataQuery: string;
    let params: unknown[];

    if (busca) {
      countQuery = `SELECT COUNT(*) as total FROM produtos WHERE nome LIKE '%' || ? || '%'`;
      dataQuery = `SELECT * FROM produtos WHERE nome LIKE '%' || ? || '%' ORDER BY id LIMIT ? OFFSET ?`;
      params = [busca];
    } else {
      countQuery = `SELECT COUNT(*) as total FROM produtos`;
      dataQuery = `SELECT * FROM produtos ORDER BY id LIMIT ? OFFSET ?`;
      params = [];
    }

    const countResult = db.query<CountResult, unknown[]>(countQuery).get(...params as []);
    const total = countResult?.total ?? 0;

    const data = db
      .query<Produto, unknown[]>(dataQuery)
      .all(...(busca ? [busca, limit, offset] : [limit, offset]) as []);

    const totalPages = Math.floor(total / limit);

    return {
      data,
      page,
      limit,
      total,
      totalPages,
    };
  }, {
    query: t.Object({
      busca: t.Optional(t.String()),
      page: t.Optional(t.String()),
      limit: t.Optional(t.String()),
    }),
  })

  .get("/:id", ({ params, set }) => {
    const produto = db
      .query<Produto, [number]>("SELECT * FROM produtos WHERE id = ?")
      .get(Number(params.id));

    if (!produto) {
      set.status = 404;
      return { error: "Produto não encontrado" };
    }

    return produto;
  })

  .post("/", ({ body, set }) => {
    const { nome, descricao, preco, estoque, categoria } = body as {
      nome: string;
      descricao?: string;
      preco: number;
      estoque?: number;
      categoria?: string;
    };

    if (!nome || nome.trim() === "") {
      set.status = 400;
      return { error: "Nome é obrigatório" };
    }

    if (preco === undefined || preco === null) {
      set.status = 400;
      return { error: "Preço é obrigatório" };
    }

    const result = db
      .query<{ id: number }, [string, string | null, number, number, string | null]>(
        `INSERT INTO produtos (nome, descricao, preco, estoque, categoria)
         VALUES (?, ?, ?, ?, ?)
         RETURNING id`
      )
      .get(
        nome,
        descricao ?? null,
        preco,
        estoque ?? 0,
        categoria ?? null
      );

    const novoProduto = db
      .query<Produto, [number]>("SELECT * FROM produtos WHERE id = ?")
      .get(result!.id);

    set.status = 201;
    return novoProduto;
  }, {
    body: t.Object({
      nome: t.String(),
      descricao: t.Optional(t.String()),
      preco: t.Number(),
      estoque: t.Optional(t.Number()),
      categoria: t.Optional(t.String()),
    }),
  })

  .put("/:id", ({ params, body, set }) => {
    const id = Number(params.id);

    const existing = db
      .query<Produto, [number]>("SELECT * FROM produtos WHERE id = ?")
      .get(id);

    if (!existing) {
      set.status = 404;
      return { error: "Produto não encontrado" };
    }

    const { nome, descricao, preco, estoque, categoria } = body as {
      nome?: string;
      descricao?: string;
      preco?: number;
      estoque?: number;
      categoria?: string;
    };

    const updatedNome = nome ?? existing.nome;
    const updatedDescricao = descricao !== undefined ? descricao : existing.descricao;
    const updatedPreco = preco !== undefined ? preco : existing.preco;
    const updatedEstoque = estoque !== undefined ? estoque : existing.estoque;
    const updatedCategoria = categoria !== undefined ? categoria : existing.categoria;

    db.run(
      `UPDATE produtos SET nome = ?, descricao = ?, preco = ?, estoque = ?, categoria = ? WHERE id = ?`,
      [updatedNome, updatedDescricao, updatedPreco, updatedEstoque, updatedCategoria, id]
    );

    const updated = db
      .query<Produto, [number]>("SELECT * FROM produtos WHERE id = ?")
      .get(id);

    return updated;
  }, {
    body: t.Object({
      nome: t.Optional(t.String()),
      descricao: t.Optional(t.String()),
      preco: t.Optional(t.Number()),
      estoque: t.Optional(t.Number()),
      categoria: t.Optional(t.String()),
    }),
  })

  .delete("/:id", ({ params, set }) => {
    const id = Number(params.id);

    const existing = db
      .query<Produto, [number]>("SELECT * FROM produtos WHERE id = ?")
      .get(id);

    if (!existing) {
      set.status = 404;
      return { error: "Produto não encontrado" };
    }

    db.run("DELETE FROM produtos WHERE id = ?", [id]);
    set.status = 204;
    return null;
  });
