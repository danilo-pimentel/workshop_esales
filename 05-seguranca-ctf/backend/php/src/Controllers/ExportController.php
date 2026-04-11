<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use App\JwtManager;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class ExportController
{
    public function export(Request $request, Response $response, array $args): Response
    {
        $claims = JwtManager::fromRequest($request);
        if ($claims === null) {
            $response->getBody()->write(json_encode([
                'error' => 'Unauthorized',
                'message' => 'Token nao fornecido',
            ]));
            return $response->withStatus(401)->withHeader('Content-Type', 'application/json');
        }

        $format = $args['format'];

        $templatePath = dirname(__DIR__, 2) . "/templates/$format";

        try {
            $template = file_get_contents($templatePath);
            if ($template === false) {
                throw new \RuntimeException("File not found");
            }
        } catch (\Throwable $e) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => "Template '$format' nao encontrado",
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        Database::logRequest([
            'method' => 'GET', 'path' => "/api/export/$format",
            'queryParams' => '', 'body' => '',
            'statusCode' => 200, 'sqlQuery' => '',
            'responsePreview' => "Template $format loaded (" . strlen($template) . " bytes)",
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'format'   => $format,
            'template' => $template,
            'message'  => 'Template carregado',
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
