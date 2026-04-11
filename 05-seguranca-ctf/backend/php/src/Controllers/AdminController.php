<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use App\JwtManager;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class AdminController
{
    public function users(Request $request, Response $response): Response
    {
        $claims = self::requireAdmin($request);
        if ($claims === null) {
            Database::logRequest([
                'method' => 'GET', 'path' => '/api/admin/users',
                'queryParams' => '', 'body' => '',
                'statusCode' => 403, 'sqlQuery' => '',
                'responsePreview' => 'Forbidden',
                'ip' => self::getIp($request),
            ]);
            $response->getBody()->write(json_encode([
                'error' => 'Forbidden',
                'message' => 'Acesso negado',
            ]));
            return $response->withStatus(403)->withHeader('Content-Type', 'application/json');
        }

        $db = Database::getInstance();
        $sqlQuery = "SELECT id, nome, email, password, role, created_at FROM users ORDER BY id";
        $users = $db->query($sqlQuery)->fetchAll();

        Database::logRequest([
            'method' => 'GET', 'path' => '/api/admin/users',
            'queryParams' => '', 'body' => '',
            'statusCode' => 200, 'sqlQuery' => $sqlQuery,
            'responsePreview' => count($users) . ' users returned',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'users' => $users,
            'count' => count($users),
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function createUser(Request $request, Response $response): Response
    {
        $claims = self::requireAdmin($request);
        if ($claims === null) {
            $response->getBody()->write(json_encode([
                'error' => 'Forbidden',
                'message' => 'Acesso negado',
            ]));
            return $response->withStatus(403)->withHeader('Content-Type', 'application/json');
        }

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

        $nome     = $body['nome'] ?? '';
        $email    = $body['email'] ?? '';
        $password = $body['password'] ?? '';
        $newRole  = $body['role'] ?? 'user';

        if (!$nome || !$email || !$password) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'Campos obrigatorios: nome, email, password',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $db = Database::getInstance();
        try {
            $stmt = $db->prepare("INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, ?)");
            $stmt->execute([$nome, $email, $password, $newRole]);
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'error' => 'Database error',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(409)->withHeader('Content-Type', 'application/json');
        }

        $stmt = $db->prepare("SELECT id, nome, email, role FROM users WHERE email = ?");
        $stmt->execute([$email]);
        $newUser = $stmt->fetch();

        Database::logRequest([
            'method' => 'POST', 'path' => '/api/admin/users',
            'queryParams' => '', 'body' => json_encode($body),
            'statusCode' => 201,
            'sqlQuery' => "INSERT INTO users (nome, email, password, role) VALUES ('$nome', '$email', '***', '$newRole')",
            'responsePreview' => json_encode($newUser),
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message' => 'Usuario criado com sucesso',
            'user' => $newUser,
        ]));
        return $response->withStatus(201)->withHeader('Content-Type', 'application/json');
    }

    public function deleteUser(Request $request, Response $response, array $args): Response
    {
        $claims = self::requireAdmin($request);
        if ($claims === null) {
            $response->getBody()->write(json_encode([
                'error' => 'Forbidden',
                'message' => 'Acesso negado',
            ]));
            return $response->withStatus(403)->withHeader('Content-Type', 'application/json');
        }

        $id = $args['id'];
        $db = Database::getInstance();

        try {
            $stmt = $db->prepare("DELETE FROM users WHERE id = ?");
            $stmt->execute([$id]);
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'error' => 'Database error',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }

        Database::logRequest([
            'method' => 'DELETE', 'path' => "/api/admin/users/$id",
            'queryParams' => '', 'body' => '',
            'statusCode' => 200, 'sqlQuery' => "DELETE FROM users WHERE id = $id",
            'responsePreview' => "user $id deleted",
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message' => "Usuario $id removido com sucesso",
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function resetDb(Request $request, Response $response): Response
    {
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

        $resetKey = 'esales-ai-reset-2026';
        if (($body['key'] ?? '') !== $resetKey) {
            $response->getBody()->write(json_encode([
                'error' => 'Forbidden',
                'message' => 'Chave de reset invalida',
            ]));
            return $response->withStatus(403)->withHeader('Content-Type', 'application/json');
        }

        try {
            $db = Database::getInstance();
            $db->exec("DELETE FROM request_logs");
            $db->exec("DELETE FROM reviews");
            $db->exec("DELETE FROM orders");
            $db->exec("DELETE FROM coupons");
            $db->exec("DELETE FROM users");
            $db->exec("DELETE FROM sqlite_sequence");

            Database::seed($db);

            $userTotal = (int) $db->query("SELECT COUNT(*) FROM users")->fetchColumn();

            $response->getBody()->write(json_encode([
                'message'  => 'Database restaurado ao estado inicial',
                'users'    => $userTotal,
                'products' => 30,
                'orders'   => 50,
                'reviews'  => 5,
                'coupons'  => 3,
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'error' => 'Reset failed',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }
    }

    private static function requireAdmin(Request $request): ?array
    {
        $claims = JwtManager::fromRequest($request);
        if ($claims === null) {
            return null;
        }
        if ($claims['role'] !== 'admin') {
            return null;
        }
        return $claims;
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
