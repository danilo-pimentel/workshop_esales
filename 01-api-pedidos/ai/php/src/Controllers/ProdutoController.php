<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class ProdutoController
{
    private PDO $db;

    public function __construct(?PDO $db = null)
    {
        $this->db = $db ?? Database::getInstance();
    }

    public function index(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $busca = $params['busca'] ?? '';
        $page = max(1, (int)($params['page'] ?? 1));
        $limit = max(1, min(100, (int)($params['limit'] ?? 10)));
        $offset = ($page - 1) * $limit;

        if ($busca !== '') {
            $countStmt = $this->db->prepare(
                "SELECT COUNT(*) FROM produtos WHERE nome LIKE '%' || :busca || '%'"
            );
            $countStmt->execute([':busca' => $busca]);
            $total = (int)$countStmt->fetchColumn();

            $stmt = $this->db->prepare(
                "SELECT * FROM produtos WHERE nome LIKE '%' || :busca || '%' ORDER BY id LIMIT :limit OFFSET :offset"
            );
            $stmt->bindValue(':busca', $busca, PDO::PARAM_STR);
            $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
            $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
            $stmt->execute();
        } else {
            $countStmt = $this->db->query("SELECT COUNT(*) FROM produtos");
            $total = (int)$countStmt->fetchColumn();

            $stmt = $this->db->prepare(
                "SELECT * FROM produtos ORDER BY id LIMIT :limit OFFSET :offset"
            );
            $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
            $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
            $stmt->execute();
        }

        $data = $stmt->fetchAll();
        $totalPages = intdiv($total, $limit);

        $payload = json_encode([
            'data'       => $data,
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
        $stmt = $this->db->prepare("SELECT * FROM produtos WHERE id = :id");
        $stmt->execute([':id' => $id]);
        $produto = $stmt->fetch();

        if (!$produto) {
            $response->getBody()->write(json_encode(['error' => 'Produto não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        $response->getBody()->write(json_encode($produto));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function create(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();

        $nome = isset($body['nome']) ? trim((string)$body['nome']) : '';
        $preco = $body['preco'] ?? null;

        if ($nome === '') {
            $response->getBody()->write(json_encode(['error' => 'Nome é obrigatório']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        if ($preco === null || $preco === '') {
            $response->getBody()->write(json_encode(['error' => 'Preço é obrigatório']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $descricao = isset($body['descricao']) ? trim((string)$body['descricao']) : null;
        $estoque = isset($body['estoque']) ? (int)$body['estoque'] : 0;
        $categoria = isset($body['categoria']) ? trim((string)$body['categoria']) : null;

        $stmt = $this->db->prepare(
            "INSERT INTO produtos (nome, descricao, preco, estoque, categoria) VALUES (:nome, :descricao, :preco, :estoque, :categoria)"
        );
        $stmt->execute([
            ':nome'      => $nome,
            ':descricao' => $descricao,
            ':preco'     => (float)$preco,
            ':estoque'   => $estoque,
            ':categoria' => $categoria,
        ]);

        $id = (int)$this->db->lastInsertId();
        $stmtGet = $this->db->prepare("SELECT * FROM produtos WHERE id = :id");
        $stmtGet->execute([':id' => $id]);
        $produto = $stmtGet->fetch();

        $response->getBody()->write(json_encode($produto));
        return $response->withHeader('Content-Type', 'application/json')->withStatus(201);
    }

    public function update(Request $request, Response $response, array $args): Response
    {
        $id = (int)$args['id'];
        $stmt = $this->db->prepare("SELECT * FROM produtos WHERE id = :id");
        $stmt->execute([':id' => $id]);
        $produto = $stmt->fetch();

        if (!$produto) {
            $response->getBody()->write(json_encode(['error' => 'Produto não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        $body = $request->getParsedBody();

        $nome = isset($body['nome']) ? (string)$body['nome'] : $produto['nome'];
        $descricao = array_key_exists('descricao', $body ?? []) ? (string)$body['descricao'] : $produto['descricao'];
        $preco = isset($body['preco']) ? (float)$body['preco'] : (float)$produto['preco'];
        $estoque = isset($body['estoque']) ? (int)$body['estoque'] : (int)$produto['estoque'];
        $categoria = array_key_exists('categoria', $body ?? []) ? (string)$body['categoria'] : $produto['categoria'];

        $stmtUpdate = $this->db->prepare(
            "UPDATE produtos SET nome = :nome, descricao = :descricao, preco = :preco, estoque = :estoque, categoria = :categoria WHERE id = :id"
        );
        $stmtUpdate->execute([
            ':nome'      => $nome,
            ':descricao' => $descricao,
            ':preco'     => $preco,
            ':estoque'   => $estoque,
            ':categoria' => $categoria,
            ':id'        => $id,
        ]);

        $stmtGet = $this->db->prepare("SELECT * FROM produtos WHERE id = :id");
        $stmtGet->execute([':id' => $id]);
        $updated = $stmtGet->fetch();

        $response->getBody()->write(json_encode($updated));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function delete(Request $request, Response $response, array $args): Response
    {
        $id = (int)$args['id'];
        $stmt = $this->db->prepare("SELECT id FROM produtos WHERE id = :id");
        $stmt->execute([':id' => $id]);

        if (!$stmt->fetch()) {
            $response->getBody()->write(json_encode(['error' => 'Produto não encontrado']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        $stmtDel = $this->db->prepare("DELETE FROM produtos WHERE id = :id");
        $stmtDel->execute([':id' => $id]);

        return $response->withStatus(204);
    }
}
