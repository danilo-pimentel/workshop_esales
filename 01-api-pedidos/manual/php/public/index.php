<?php

declare(strict_types=1);

use App\Controllers\ClienteController;
use App\Controllers\DashboardController;
use App\Controllers\PedidoController;
use App\Controllers\ProdutoController;
use App\Middleware\CorsMiddleware;
use Slim\Factory\AppFactory;

require __DIR__ . '/../vendor/autoload.php';

$app = AppFactory::create();

// Middleware
$app->addBodyParsingMiddleware();
$app->addRoutingMiddleware();
$app->addErrorMiddleware(true, true, true);
$app->add(CorsMiddleware::class);

// OPTIONS preflight for all routes
$app->options('/{routes:.+}', function ($request, $response) {
    return $response;
});

// ─── Produtos ───────────────────────────────────────────────────────────────
$app->get('/api/produtos', [ProdutoController::class, 'index']);
$app->get('/api/produtos/{id:[0-9]+}', [ProdutoController::class, 'show']);
$app->post('/api/produtos', [ProdutoController::class, 'create']);
$app->put('/api/produtos/{id:[0-9]+}', [ProdutoController::class, 'update']);
$app->delete('/api/produtos/{id:[0-9]+}', [ProdutoController::class, 'delete']);

// ─── Pedidos ────────────────────────────────────────────────────────────────
$app->get('/api/pedidos', [PedidoController::class, 'index']);
$app->get('/api/pedidos/{id:[0-9]+}', [PedidoController::class, 'show']);
$app->post('/api/pedidos', [PedidoController::class, 'create']);
$app->patch('/api/pedidos/{id:[0-9]+}/status', [PedidoController::class, 'updateStatus']);

// ─── Clientes ───────────────────────────────────────────────────────────────
$app->get('/api/clientes', [ClienteController::class, 'index']);
$app->get('/api/clientes/{id:[0-9]+}', [ClienteController::class, 'show']);

// ─── Dashboard ──────────────────────────────────────────────────────────────
$app->get('/api/dashboard/resumo', [DashboardController::class, 'resumo']);

$app->run();
