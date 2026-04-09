<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class PedidoController
{
    private PDO $db;

    private const STATUS_VALIDOS = ['pendente', 'processando', 'enviado', 'entregue', 'cancelado'];

    public function __construct(?PDO $db = null)
    {
        $this->db = $db ?? Database::getInstance();
    }

    public function index(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $page = max(1, (int)($params['page'] ?? 1));
        $limit = max(1, min(100, (int)($params['limit'] ?? 10)));
        $offset = ($page - 1) * $limit;


        $countStmt = $this->db->query("SELECT COUNT(*) FROM pedidos");
        $total = (int)$countStmt->fetchColumn();
        $totalPages = intdiv($total, $limit);

        $stmt = $this->db->prepare(
            "SELECT p.*, c.nome AS cliente_nome
             FROM pedidos p
             JOIN clientes c ON c.id = p.cliente_id
             ORDER BY p.id DESC
             LIMIT :limit OFFSET :offset"
        );
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        $pedidos = $stmt->fetchAll();

        $payload = json_encode([
            'data'       => $pedidos,
            'page'       => $page,
            'limit'      => $limit,
            'total'      => $total,
            'totalPages' => $totalPages,
        ]);

        $response->getBody()->write($payload);
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $id = (int)$args['id'];

        $stmtPedido = $this->db->prepare(
            "SELECT p.*, c.nome AS cliente_nome, c.email AS cliente_email, c.telefone AS cliente_telefone
             FROM pedidos p
             JOIN clientes c ON c.id = p.cliente_id
             WHERE p.id = :id"
        );
        $stmtPedido->execute([':id' => $id]);
        $pedido = $stmtPedido->fetch();

        if (!$pedido) {
            $response->getBody()->write(json_encode(['error' => 'Pedido não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        $stmtItens = $this->db->prepare(
            "SELECT ip.*, pr.nome AS produto_nome
             FROM itens_pedido ip
             JOIN produtos pr ON pr.id = ip.produto_id
             WHERE ip.pedido_id = :pedido_id"
        );
        $stmtItens->execute([':pedido_id' => $id]);
        $pedido['itens'] = $stmtItens->fetchAll();

        $response->getBody()->write(json_encode($pedido));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function create(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();

        $clienteId = $body['cliente_id'] ?? null;
        $itens = $body['itens'] ?? null;

        // Validate client_id exists
        if ($clienteId === null) {
            $response->getBody()->write(json_encode(['error' => 'Cliente não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $clienteId = (int)$clienteId;

        $stmtCliente = $this->db->prepare("SELECT id FROM clientes WHERE id = :id");
        $stmtCliente->execute([':id' => $clienteId]);
        if (!$stmtCliente->fetch()) {
            $response->getBody()->write(json_encode(['error' => 'Cliente não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        if (!is_array($itens) || count($itens) === 0) {
            $response->getBody()->write(json_encode(['error' => 'Pedido deve ter pelo menos um item']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        // Validate all products exist and have stock
        $produtoIds = array_map(fn($item) => (int)$item['produto_id'], $itens);
        $placeholders = implode(', ', array_fill(0, count($produtoIds), '?'));
        $stmtProds = $this->db->prepare(
            "SELECT id, preco, estoque, nome FROM produtos WHERE id IN ({$placeholders})"
        );
        $stmtProds->execute($produtoIds);
        $produtos = $stmtProds->fetchAll();

        if (count($produtos) !== count($produtoIds)) {
            $response->getBody()->write(json_encode(['error' => 'Um ou mais produtos não encontrados']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $produtoMap = [];
        foreach ($produtos as $p) {
            $produtoMap[(int)$p['id']] = $p;
        }

        // Check stock for each item
        foreach ($itens as $item) {
            $produtoId = (int)$item['produto_id'];
            $quantidade = (int)$item['quantidade'];
            $produto = $produtoMap[$produtoId] ?? null;

            if (!$produto) {
                $response->getBody()->write(json_encode(['error' => "Produto {$produtoId} não encontrado"]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            if ((int)$produto['estoque'] < $quantidade) {
                $response->getBody()->write(json_encode(['error' => "Estoque insuficiente para o produto {$produto['nome']}"]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }
        }

        $total = 0.0;
        foreach ($itens as $item) {
            $produto = $produtoMap[(int)$item['produto_id']];
            $total += (float)$produto['preco'];
        }

        // Insert pedido
        $stmtPedido = $this->db->prepare(
            "INSERT INTO pedidos (cliente_id, status, total) VALUES (:cliente_id, 'pendente', :total)"
        );
        $stmtPedido->execute([':cliente_id' => $clienteId, ':total' => $total]);
        $pedidoId = (int)$this->db->lastInsertId();

        // Insert items and decrement stock
        $stmtItem = $this->db->prepare(
            "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario) VALUES (:pedido_id, :produto_id, :quantidade, :preco_unitario)"
        );
        $stmtUpdateStock = $this->db->prepare(
            "UPDATE produtos SET estoque = estoque - :quantidade WHERE id = :id"
        );

        foreach ($itens as $item) {
            $produtoId = (int)$item['produto_id'];
            $quantidade = (int)$item['quantidade'];
            $produto = $produtoMap[$produtoId];

            $stmtItem->execute([
                ':pedido_id'     => $pedidoId,
                ':produto_id'    => $produtoId,
                ':quantidade'    => $quantidade,
                ':preco_unitario' => (float)$produto['preco'],
            ]);

            $stmtUpdateStock->execute([
                ':quantidade' => $quantidade,
                ':id'         => $produtoId,
            ]);
        }

        // Return bare pedido only (no items, no client info)
        $stmtGet = $this->db->prepare("SELECT * FROM pedidos WHERE id = :id");
        $stmtGet->execute([':id' => $pedidoId]);
        $novoPedido = $stmtGet->fetch();

        $response->getBody()->write(json_encode($novoPedido));
        return $response->withHeader('Content-Type', 'application/json')->withStatus(201);
    }

    public function updateStatus(Request $request, Response $response, array $args): Response
    {
        $id = (int)$args['id'];

        $stmtCheck = $this->db->prepare("SELECT id, status FROM pedidos WHERE id = :id");
        $stmtCheck->execute([':id' => $id]);
        $pedido = $stmtCheck->fetch();

        if (!$pedido) {
            $response->getBody()->write(json_encode(['error' => 'Pedido não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        $body = $request->getParsedBody();

        if (!isset($body['status'])) {
            $response->getBody()->write(json_encode(['error' => 'Campo obrigatório: status']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $novoStatus = (string)$body['status'];

        if (!in_array($novoStatus, self::STATUS_VALIDOS, true)) {
            $response->getBody()->write(json_encode([
                'error' => 'Status inválido. Valores permitidos: ' . implode(', ', self::STATUS_VALIDOS),
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $stmtUpdate = $this->db->prepare("UPDATE pedidos SET status = :status WHERE id = :id");
        $stmtUpdate->execute([':status' => $novoStatus, ':id' => $id]);

        $stmtGet = $this->db->prepare("SELECT * FROM pedidos WHERE id = :id");
        $stmtGet->execute([':id' => $id]);
        $updated = $stmtGet->fetch();

        $response->getBody()->write(json_encode($updated));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
