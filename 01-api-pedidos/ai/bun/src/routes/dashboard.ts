import Elysia from "elysia";
import { db } from "../db";

interface ResumoResult {
  total_vendas: number;
  total_pedidos: number;
  ticket_medio: number;
}

interface StatusCount {
  status: string;
  quantidade: number;
}

interface TopProduto {
  produto_id: number;
  produto_nome: string;
  total_vendido: number;
  receita: number;
}

export const dashboardRoutes = new Elysia({ prefix: "/api/dashboard" })
  .get("/resumo", () => {
    const resumo = db.query<ResumoResult, []>(`
      SELECT
        COALESCE(SUM(total), 0) as total_vendas,
        COUNT(*) as total_pedidos,
        CASE
          WHEN COUNT(*) > 0 THEN ROUND(SUM(total) / COUNT(*), 2)
          ELSE 0
        END as ticket_medio
      FROM pedidos
      WHERE status != 'cancelado'
    `).get();

    const porStatus = db.query<StatusCount, []>(`
      SELECT status, COUNT(*) as quantidade
      FROM pedidos
      GROUP BY status
      ORDER BY quantidade DESC
    `).all();

    const topProdutos = db.query<TopProduto, []>(`
      SELECT
        ip.produto_id,
        pr.nome as produto_nome,
        SUM(ip.quantidade) as total_vendido,
        ROUND(SUM(ip.quantidade * ip.preco_unitario), 2) as receita
      FROM itens_pedido ip
      JOIN produtos pr ON pr.id = ip.produto_id
      JOIN pedidos p ON p.id = ip.pedido_id
      WHERE p.status != 'cancelado'
      GROUP BY ip.produto_id, pr.nome
      ORDER BY total_vendido DESC
      LIMIT 10
    `).all();

    return {
      total_vendas: Number((resumo?.total_vendas ?? 0).toFixed(2)),
      total_pedidos: resumo?.total_pedidos ?? 0,
      ticket_medio: resumo?.ticket_medio ?? 0,
      por_status: porStatus,
      top_produtos: topProdutos,
    };
  });
