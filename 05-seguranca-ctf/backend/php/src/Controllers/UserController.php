<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class UserController
{
    public function me(Request $request, Response $response): Response
    {
        $claims = $request->getAttribute('jwt_claims');
        $db = Database::getInstance();

        $sqlQuery = "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = {$claims['sub']}";

        $stmt = $db->prepare("SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?");
        $stmt->execute([$claims['sub']]);
        $user = $stmt->fetch();

        Database::logRequest([
            'method' => 'GET', 'path' => '/api/users/me',
            'queryParams' => '', 'body' => '',
            'statusCode' => $user ? 200 : 404, 'sqlQuery' => $sqlQuery,
            'responsePreview' => $user ? 'found' : 'Not found',
            'ip' => self::getIp($request),
        ]);

        if (!$user) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => 'Usuario nao encontrado',
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        $response->getBody()->write(json_encode($user));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $id = $args['id'];
        $db = Database::getInstance();

        $sqlQuery = "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = $id";

        try {
            $stmt = $db->prepare("SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?");
            $stmt->execute([$id]);
            $user = $stmt->fetch();
        } catch (\Exception $e) {
            Database::logRequest([
                'method' => 'GET', 'path' => "/api/users/$id",
                'queryParams' => '', 'body' => '',
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
            'method' => 'GET', 'path' => "/api/users/$id",
            'queryParams' => '', 'body' => '',
            'statusCode' => $user ? 200 : 404, 'sqlQuery' => $sqlQuery,
            'responsePreview' => $user ? "user $id" : 'Not found',
            'ip' => self::getIp($request),
        ]);

        if (!$user) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => 'Usuario nao encontrado',
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        $response->getBody()->write(json_encode($user));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function update(Request $request, Response $response, array $args): Response
    {
        $claims = $request->getAttribute('jwt_claims');
        $id = $args['id'];

        if ((string) $id !== (string) $claims['sub'] && $claims['role'] !== 'admin') {
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

        $nome     = $body['nome'] ?? null;
        $email    = $body['email'] ?? null;
        $password = $body['password'] ?? null;
        $telefone = $body['telefone'] ?? null;
        $endereco = $body['endereco'] ?? null;

        $db = Database::getInstance();

        try {
            $stmt = $db->prepare(
                "UPDATE users SET
                    nome     = COALESCE(?, nome),
                    email    = COALESCE(?, email),
                    password = COALESCE(?, password),
                    telefone = COALESCE(?, telefone),
                    endereco = COALESCE(?, endereco)
                WHERE id = ?"
            );
            $stmt->execute([$nome, $email, $password, $telefone, $endereco, $id]);
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'error' => 'Database error',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }

        Database::logRequest([
            'method' => 'PUT', 'path' => "/api/users/$id",
            'queryParams' => '', 'body' => json_encode($body),
            'statusCode' => 200, 'sqlQuery' => "UPDATE users SET ... WHERE id = $id",
            'responsePreview' => 'Updated',
            'ip' => self::getIp($request),
        ]);

        $stmt = $db->prepare("SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?");
        $stmt->execute([$id]);
        $updated = $stmt->fetch();

        $response->getBody()->write(json_encode([
            'message' => 'Usuario atualizado',
            'user' => $updated,
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
