<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use App\JwtManager;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class ProductController
{
    public function index(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $page   = max(1, (int) ($params['page'] ?? 1));
        $limit  = max(1, (int) ($params['limit'] ?? 10));
        $offset = ($page - 1) * $limit;

        $db = Database::getInstance();

        $sqlQuery = "SELECT * FROM products LIMIT $limit OFFSET $offset";

        $stmt = $db->prepare("SELECT * FROM products LIMIT ? OFFSET ?");
        $stmt->execute([$limit, $offset]);
        $products = $stmt->fetchAll();

        $total = (int) $db->query("SELECT COUNT(*) FROM products")->fetchColumn();

        Database::logRequest([
            'method' => 'GET', 'path' => '/api/products',
            'queryParams' => $request->getUri()->getQuery(), 'body' => '',
            'statusCode' => 200, 'sqlQuery' => $sqlQuery,
            'responsePreview' => count($products) . ' products',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'products' => $products,
            'total'    => $total,
            'page'     => $page,
            'limit'    => $limit,
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function search(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $rawSearch = $params['q'] ?? '';

        $search = self::sanitizeInput($rawSearch);

        $sqlQuery = "SELECT id, nome, descricao, preco, categoria, created_at FROM products WHERE nome LIKE '%" . $search . "%'";

        $db = Database::getInstance();

        try {
            $result = $db->query($sqlQuery);
            $results = $result ? $result->fetchAll() : [];
        } catch (\Exception $e) {
            Database::logRequest([
                'method' => 'GET', 'path' => '/api/products/search',
                'queryParams' => $request->getUri()->getQuery(), 'body' => '',
                'statusCode' => 500, 'sqlQuery' => $sqlQuery,
                'responsePreview' => $e->getMessage(),
                'ip' => self::getIp($request),
            ]);
            $response->getBody()->write(json_encode([
                'error' => 'Database error',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }

        Database::logRequest([
            'method' => 'GET', 'path' => '/api/products/search',
            'queryParams' => $request->getUri()->getQuery(), 'body' => '',
            'statusCode' => 200, 'sqlQuery' => $sqlQuery,
            'responsePreview' => count($results) . ' results',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'results' => $results,
            'count'   => count($results),
            'query'   => $search,
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $id = $args['id'];
        $db = Database::getInstance();

        $sqlQuery = "SELECT * FROM products WHERE id = $id";

        try {
            $stmt = $db->prepare("SELECT * FROM products WHERE id = ?");
            $stmt->execute([$id]);
            $product = $stmt->fetch();
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'error' => 'Database error',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }

        Database::logRequest([
            'method' => 'GET', 'path' => "/api/products/$id",
            'queryParams' => '', 'body' => '',
            'statusCode' => $product ? 200 : 404, 'sqlQuery' => $sqlQuery,
            'responsePreview' => $product ? 'found' : 'not found',
            'ip' => self::getIp($request),
        ]);

        if (!$product) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => 'Produto nao encontrado',
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        $response->getBody()->write(json_encode($product));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getReviews(Request $request, Response $response, array $args): Response
    {
        $id = $args['id'];
        $db = Database::getInstance();

        $stmt = $db->prepare("SELECT * FROM reviews WHERE product_id = ? ORDER BY created_at DESC");
        $stmt->execute([$id]);
        $reviews = $stmt->fetchAll();

        Database::logRequest([
            'method' => 'GET', 'path' => "/api/products/$id/reviews",
            'queryParams' => '', 'body' => '',
            'statusCode' => 200, 'sqlQuery' => "SELECT * FROM reviews WHERE product_id = $id",
            'responsePreview' => count($reviews) . ' reviews',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'reviews' => $reviews,
            'count'   => count($reviews),
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function createReview(Request $request, Response $response, array $args): Response
    {
        $claims = $request->getAttribute('jwt_claims');
        $id = $args['id'];

        $body = $request->getParsedBody();
        if ($body === null) {
            $raw = (string) $request->getBody();
            $body = json_decode($raw, true);
        }
        if (!is_array($body)) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'JSON invalido',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $text   = $body['text'] ?? '';
        $rating = $body['rating'] ?? 5;

        if (!$text) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => "Campo 'text' obrigatorio",
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        if ($rating < 1 || $rating > 5) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'Rating deve ser entre 1 e 5',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $db = Database::getInstance();
        $stmt = $db->prepare("SELECT nome FROM users WHERE id = ?");
        $stmt->execute([$claims['sub']]);
        $userRow = $stmt->fetch();
        $userName = $userRow ? $userRow['nome'] : 'Usuario';

        $stmt = $db->prepare("INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)");
        $stmt->execute([$id, $claims['sub'], $userName, $rating, $text]);

        $reviewId = $db->lastInsertId();
        $stmt = $db->prepare("SELECT * FROM reviews WHERE id = ?");
        $stmt->execute([$reviewId]);
        $review = $stmt->fetch();

        Database::logRequest([
            'method' => 'POST', 'path' => "/api/products/$id/reviews",
            'queryParams' => '', 'body' => json_encode($body),
            'statusCode' => 201,
            'sqlQuery' => "INSERT INTO reviews (product_id, user_id, ...) VALUES ($id, {$claims['sub']}, ...)",
            'responsePreview' => 'review created',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message' => 'Review adicionada',
            'review' => $review,
        ]));
        return $response->withStatus(201)->withHeader('Content-Type', 'application/json');
    }

    private static function sanitizeInput(string $input): string
    {
        return str_replace(['<', '>', '"', '&'], '', $input);
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
