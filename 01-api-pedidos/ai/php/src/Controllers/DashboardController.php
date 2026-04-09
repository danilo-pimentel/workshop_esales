<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class DashboardController
{
    private PDO $db;

    public function __construct(?PDO $db = null)
    {
        $this->db = $db ?? Database::getInstance();
    }

    public function resumo(Request $request, Response $response): Response
    {
        $stmtResumo = $this->db->query(
            "SELECT
                COALESCE(SUM(total), 0) AS total_vendas,
                COUNT(*) AS total_pedidos,
                CASE
                    WHEN COUNT(*) > 0 THEN ROUND(SUM(total) / COUNT(*), 2)
                    ELSE 0
                END AS ticket_medio
             FROM pedidos
             WHERE status != 'cancelado'"
        );
        $resumo = $stmtResumo->fetch();

        $stmtPorStatus = $this->db->query(
            "SELECT status, COUNT(*) AS quantidade
             FROM pedidos
             GROUP BY status
             ORDER BY quantidade DESC"
        );
        $porStatus = $stmtPorStatus->fetchAll();

        $stmtTopProdutos = $this->db->query(
            "SELECT
                ip.produto_id,
                pr.nome AS produto_nome,
                SUM(ip.quantidade) AS total_vendido,
                ROUND(SUM(ip.quantidade * ip.preco_unitario), 2) AS receita
             FROM itens_pedido ip
             JOIN produtos pr ON pr.id = ip.produto_id
             JOIN pedidos p ON p.id = ip.pedido_id
             WHERE p.status != 'cancelado'
             GROUP BY ip.produto_id, pr.nome
             ORDER BY total_vendido DESC
             LIMIT 10"
        );
        $topProdutos = $stmtTopProdutos->fetchAll();

        $payload = json_encode([
            'total_vendas'  => round((float)($resumo['total_vendas'] ?? 0), 2),
            'total_pedidos' => (int)($resumo['total_pedidos'] ?? 0),
            'ticket_medio'  => (float)($resumo['ticket_medio'] ?? 0),
            'por_status'    => $porStatus,
            'top_produtos'  => $topProdutos,
        ]);

        $response->getBody()->write($payload);
        return $response->withHeader('Content-Type', 'application/json');
    }
}
