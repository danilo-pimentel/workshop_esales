<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class ClienteController
{
    private PDO $db;

    public function __construct(?PDO $db = null)
    {
        $this->db = $db ?? Database::getInstance();
    }

    public function index(Request $request, Response $response): Response
    {
        $stmt = $this->db->query("SELECT * FROM clientes ORDER BY nome ASC");
        $clientes = $stmt->fetchAll();

        $response->getBody()->write(json_encode($clientes));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $id = (int)$args['id'];

        $stmt = $this->db->prepare("SELECT * FROM clientes WHERE id = :id");
        $stmt->execute([':id' => $id]);
        $cliente = $stmt->fetch();

        if (!$cliente) {
            $response->getBody()->write(json_encode(['error' => 'Cliente não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        $stmtPedidos = $this->db->prepare(
            "SELECT * FROM pedidos WHERE cliente_id = :cliente_id ORDER BY created_at DESC"
        );
        $stmtPedidos->execute([':cliente_id' => $id]);
        $pedidos = $stmtPedidos->fetchAll();

        $totalGasto = 0.0;
        foreach ($pedidos as $p) {
            $totalGasto += (float)$p['total'];
        }

        $cliente['pedidos'] = $pedidos;
        $cliente['total_pedidos'] = count($pedidos);
        $cliente['total_gasto'] = round($totalGasto, 2);

        $response->getBody()->write(json_encode($cliente));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
