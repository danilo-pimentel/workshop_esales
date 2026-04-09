import { test, expect, beforeAll, afterAll } from "bun:test";
import { app } from "../src/index";

const BASE_URL = "http://localhost:3000";

beforeAll(async () => {
  await new Promise((resolve) => setTimeout(resolve, 100));
});

afterAll(() => {
  app.stop();
});

test("testCrudProduto", async () => {
  // CREATE
  const createRes = await fetch(`${BASE_URL}/api/produtos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      nome: "Produto Teste CRUD",
      descricao: "Descricao do produto de teste",
      preco: 99.90,
      estoque: 10,
      categoria: "Testes",
    }),
  });
  expect(createRes.status).toBe(201);
  const created = (await createRes.json()) as { id: number; nome: string; preco: number };
  expect(created).toHaveProperty("id");
  expect(created.nome).toBe("Produto Teste CRUD");
  expect(created.preco).toBe(99.90);
  const prodId = created.id;

  // READ
  const getRes = await fetch(`${BASE_URL}/api/produtos/${prodId}`);
  expect(getRes.status).toBe(200);
  const fetched = (await getRes.json()) as { id: number; nome: string };
  expect(fetched.id).toBe(prodId);
  expect(fetched.nome).toBe("Produto Teste CRUD");

  // UPDATE
  const updateRes = await fetch(`${BASE_URL}/api/produtos/${prodId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      nome: "Produto Atualizado",
      preco: 149.90,
      estoque: 20,
    }),
  });
  expect(updateRes.status).toBe(200);
  const updated = (await updateRes.json()) as { nome: string; preco: number };
  expect(updated.nome).toBe("Produto Atualizado");
  expect(updated.preco).toBe(149.90);

  // DELETE
  const deleteRes = await fetch(`${BASE_URL}/api/produtos/${prodId}`, {
    method: "DELETE",
  });
  expect(deleteRes.status).toBe(204);

  // VERIFY DELETED
  const goneRes = await fetch(`${BASE_URL}/api/produtos/${prodId}`);
  expect(goneRes.status).toBe(404);
});

test("testBuscaCaseInsensitive", async () => {
  const res = await fetch(`${BASE_URL}/api/produtos?busca=notebook`);
  expect(res.status).toBe(200);

  const body = (await res.json()) as { data: { nome: string }[]; total: number };

  expect(body.total).toBeGreaterThan(0);
});

test("testCriarPedido", async () => {
  const res = await fetch(`${BASE_URL}/api/pedidos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      cliente_id: 1,
      itens: [
        { produto_id: 1, quantidade: 2 },
        { produto_id: 2, quantidade: 1 },
      ],
    }),
  });

  expect(res.status).toBe(201);
  const pedido = (await res.json()) as {
    id: number;
    cliente_id: number;
    status: string;
    total: number;
  };
  expect(pedido).toHaveProperty("id");
  expect(pedido.cliente_id).toBe(1);
  expect(pedido.status).toBe("pendente");
  expect(pedido.total).toBeGreaterThan(0);

  // Verify details via GET
  const detalheRes = await fetch(`${BASE_URL}/api/pedidos/${pedido.id}`);
  expect(detalheRes.status).toBe(200);
  const detalhe = (await detalheRes.json()) as {
    id: number;
    itens: unknown[];
    cliente_nome: string;
  };
  expect(detalhe.id).toBe(pedido.id);
  expect(Array.isArray(detalhe.itens)).toBe(true);
  expect(detalhe).toHaveProperty("cliente_nome");
});

test("testPrecoNegativoRejeitado", async () => {
  const res = await fetch(`${BASE_URL}/api/produtos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      nome: "Produto Preco Negativo",
      preco: -10.00,
      estoque: 5,
      categoria: "Teste",
    }),
  });

  expect(res.status).toBe(400);
});

test("testListarClientes", async () => {
  const res = await fetch(`${BASE_URL}/api/clientes`);
  expect(res.status).toBe(200);

  const clientes = (await res.json()) as { id: number; nome: string; email: string }[];
  expect(Array.isArray(clientes)).toBe(true);
  expect(clientes.length).toBe(20);

  // Detail with order history
  const detalheRes = await fetch(`${BASE_URL}/api/clientes/1`);
  expect(detalheRes.status).toBe(200);
  const cliente = (await detalheRes.json()) as {
    pedidos: unknown[];
    total_pedidos: number;
    total_gasto: number;
  };
  expect(cliente).toHaveProperty("pedidos");
  expect(cliente).toHaveProperty("total_pedidos");
  expect(cliente).toHaveProperty("total_gasto");
});

test("testPaginacaoTotalPages", async () => {
  const res = await fetch(`${BASE_URL}/api/produtos?page=1&limit=7`);
  expect(res.status).toBe(200);

  const body = (await res.json()) as { total: number; limit: number; totalPages: number };
  const expectedTotalPages = Math.ceil(body.total / body.limit);

  expect(body.totalPages).toBe(expectedTotalPages);
});

test("testDashboardResumo", async () => {
  const res = await fetch(`${BASE_URL}/api/dashboard/resumo`);
  expect(res.status).toBe(200);

  const body = (await res.json()) as {
    total_vendas: number;
    total_pedidos: number;
    ticket_medio: number;
  };

  expect(body).toHaveProperty("total_vendas");
  expect(body).toHaveProperty("total_pedidos");
  expect(body).toHaveProperty("ticket_medio");

  expect(typeof body.total_vendas).toBe("number");
  expect(typeof body.total_pedidos).toBe("number");
  expect(typeof body.ticket_medio).toBe("number");

  expect(body.total_vendas).toBeGreaterThan(0);
  expect(body.total_pedidos).toBeGreaterThan(0);
  expect(body.ticket_medio).toBeGreaterThan(0);
});

test("testTotalPedidoComQuantidade", async () => {
  const createProdRes = await fetch(`${BASE_URL}/api/produtos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      nome: "Produto Para Teste Quantidade",
      descricao: "Produto de teste",
      preco: 100.00,
      estoque: 50,
      categoria: "Teste",
    }),
  });
  expect(createProdRes.status).toBe(201);
  const prodBody = (await createProdRes.json()) as { id: number };
  const prodId = prodBody.id;

  const res = await fetch(`${BASE_URL}/api/pedidos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      cliente_id: 1,
      itens: [{ produto_id: prodId, quantidade: 3 }],
    }),
  });

  expect(res.status).toBe(201);
  const pedido = (await res.json()) as { id: number; total: number };

  expect(pedido.total).toBe(300.00);
});

test("testFiltroStatus", async () => {
  const resAll = await fetch(`${BASE_URL}/api/pedidos?limit=200`);
  expect(resAll.status).toBe(200);
  const allBody = (await resAll.json()) as { data: { status: string }[]; total: number };
  const totalGeral = allBody.total;

  const res = await fetch(`${BASE_URL}/api/pedidos?status=entregue&limit=200`);
  expect(res.status).toBe(200);
  const body = (await res.json()) as { data: { status: string }[]; total: number };

  const allEntregue = body.data.every((p) => p.status === "entregue");

  expect(allEntregue).toBe(true);
  expect(body.total).toBeLessThan(totalGeral);
});
