<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use App\JwtManager;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class LogController
{
    public function index(Request $request, Response $response): Response
    {
        $claims = JwtManager::fromRequest($request);
        if ($claims === null) {
            $response->getBody()->write(json_encode([
                'error' => 'Unauthorized',
                'message' => 'Token nao fornecido',
            ]));
            return $response->withStatus(401)->withHeader('Content-Type', 'application/json');
        }

        if ($claims['role'] !== 'admin') {
            $response->getBody()->write(json_encode([
                'error' => 'Forbidden',
                'message' => 'Acesso negado',
            ]));
            return $response->withStatus(403)->withHeader('Content-Type', 'application/json');
        }

        $params = $request->getQueryParams();
        $limit = (int) ($params['limit'] ?? 100);
        $db = Database::getInstance();

        $stmt = $db->prepare("SELECT * FROM request_logs ORDER BY id DESC LIMIT ?");
        $stmt->execute([$limit]);
        $logs = $stmt->fetchAll();

        Database::logRequest([
            'method' => 'GET', 'path' => '/api/logs',
            'queryParams' => $request->getUri()->getQuery(), 'body' => '',
            'statusCode' => 200,
            'sqlQuery' => "SELECT * FROM request_logs ORDER BY id DESC LIMIT $limit",
            'responsePreview' => count($logs) . ' log entries returned',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'logs'  => $logs,
            'count' => count($logs),
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function clear(Request $request, Response $response): Response
    {
        $claims = JwtManager::fromRequest($request);
        if ($claims === null) {
            $response->getBody()->write(json_encode([
                'error' => 'Unauthorized',
                'message' => 'Token nao fornecido',
            ]));
            return $response->withStatus(401)->withHeader('Content-Type', 'application/json');
        }

        if ($claims['role'] !== 'admin') {
            $response->getBody()->write(json_encode([
                'error' => 'Forbidden',
                'message' => 'Acesso negado',
            ]));
            return $response->withStatus(403)->withHeader('Content-Type', 'application/json');
        }

        $db = Database::getInstance();
        $db->exec("DELETE FROM request_logs");

        Database::logRequest([
            'method' => 'DELETE', 'path' => '/api/logs',
            'queryParams' => '', 'body' => '',
            'statusCode' => 200, 'sqlQuery' => 'DELETE FROM request_logs',
            'responsePreview' => 'All logs cleared',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message' => 'Todos os logs foram apagados',
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
