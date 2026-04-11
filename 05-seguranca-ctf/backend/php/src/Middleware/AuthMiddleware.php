<?php

declare(strict_types=1);

namespace App\Middleware;

use App\JwtManager;
use Psr\Http\Message\ResponseInterface;
use Psr\Http\Message\ServerRequestInterface;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface;
use Slim\Psr7\Response;

class AuthMiddleware implements MiddlewareInterface
{
    public function process(
        ServerRequestInterface $request,
        RequestHandlerInterface $handler
    ): ResponseInterface {
        $authHeader = $request->getHeaderLine('Authorization');
        $token      = '';

        if (str_starts_with($authHeader, 'Bearer ')) {
            $token = substr($authHeader, 7);
        }

        if (!$token) {
            $response = new Response();
            $response->getBody()->write(json_encode([
                'error'   => 'Unauthorized',
                'message' => 'Token nao fornecido',
            ]));
            return $response
                ->withStatus(401)
                ->withHeader('Content-Type', 'application/json');
        }

        $claims = JwtManager::decode($token);

        if ($claims === null) {
            $response = new Response();
            $response->getBody()->write(json_encode([
                'error'   => 'Unauthorized',
                'message' => 'Token invalido ou expirado',
            ]));
            return $response
                ->withStatus(401)
                ->withHeader('Content-Type', 'application/json');
        }

        $request = $request->withAttribute('jwt_claims', $claims);
        return $handler->handle($request);
    }
}
