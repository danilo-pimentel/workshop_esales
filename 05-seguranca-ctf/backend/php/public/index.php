<?php

declare(strict_types=1);

use App\Controllers\AdminController;
use App\Controllers\AuthController;
use App\Controllers\ExportController;
use App\Controllers\LogController;
use App\Controllers\OrderController;
use App\Controllers\ProductController;
use App\Controllers\UserController;
use App\Database;
use App\Middleware\AuthMiddleware;
use App\Middleware\CorsMiddleware;
use App\Middleware\LoggerMiddleware;
use Slim\Factory\AppFactory;

$autoload = dirname(__DIR__) . '/vendor/autoload.php';
if (!file_exists($autoload)) {
    http_response_code(503);
    header('Content-Type: application/json');
    echo json_encode([
        'error'   => 'Dependencies not installed',
        'message' => 'Run: composer install',
    ]);
    exit(1);
}
require $autoload;

// Delete and recreate database on startup (first request)
$dbPath = Database::getDbPath();
if (!file_exists($dbPath)) {
    Database::getInstance();
}

$app = AppFactory::create();

// Error handler
$errorMiddleware = $app->addErrorMiddleware(true, true, true);
$customErrorHandler = function (
    \Psr\Http\Message\ServerRequestInterface $request,
    \Throwable $exception,
    bool $displayErrorDetails,
    bool $logErrors,
    bool $logErrorDetails
) use ($app) {
    $response = $app->getResponseFactory()->createResponse();
    $response->getBody()->write(json_encode([
        'error'   => 'Internal Server Error',
        'message' => $exception->getMessage(),
        'stack'   => array_slice(explode("\n", $exception->getTraceAsString()), 0, 8),
    ]));
    return $response
        ->withStatus(500)
        ->withHeader('Content-Type', 'application/json')
        ->withHeader('Access-Control-Allow-Origin', '*');
};
$errorMiddleware->setDefaultErrorHandler($customErrorHandler);

// Global middleware
$app->add(new LoggerMiddleware());
$app->add(new CorsMiddleware());
$app->addBodyParsingMiddleware();
$app->addRoutingMiddleware();

// Health check
$app->get('/', function ($request, $response) {
    $response->getBody()->write(json_encode([
        'service'   => 'SecureShop API',
        'version'   => '1.0.0',
        'status'    => 'running',
        'endpoints' => [
            'auth'     => '/api/auth/login  /api/auth/register  /api/auth/forgot-password',
            'users'    => '/api/users/me  /api/users/:id',
            'products' => '/api/products  /api/products/search?q=  /api/products/:id/reviews',
            'orders'   => '/api/orders  /api/orders/:id  /api/orders/:id/apply-coupon',
            'export'   => '/api/export/:format',
            'admin'    => '/api/admin/users',
            'logs'     => '/api/logs',
        ],
    ]));
    return $response->withHeader('Content-Type', 'application/json');
});

// Auth routes (public)
$app->post('/api/auth/login',           [AuthController::class, 'login']);
$app->post('/api/auth/register',        [AuthController::class, 'register']);
$app->post('/api/auth/forgot-password', [AuthController::class, 'forgotPassword']);

// Product routes (public read)
$app->get('/api/products',              [ProductController::class, 'index']);
$app->get('/api/products/search',       [ProductController::class, 'search']);
$app->get('/api/products/{id}',         [ProductController::class, 'show']);
$app->get('/api/products/{id}/reviews', [ProductController::class, 'getReviews']);

// Product reviews (auth required)
$app->post('/api/products/{id}/reviews', [ProductController::class, 'createReview'])
    ->add(new AuthMiddleware());

// User routes (auth required)
$app->group('/api/users', function ($group) {
    $group->get('/me',    [UserController::class, 'me']);
    $group->get('/{id}',  [UserController::class, 'show']);
    $group->put('/{id}',  [UserController::class, 'update']);
})->add(new AuthMiddleware());

// Order routes (auth required)
$app->group('/api/orders', function ($group) {
    $group->get('',                    [OrderController::class, 'index']);
    $group->get('/{id}',               [OrderController::class, 'show']);
    $group->post('',                   [OrderController::class, 'create']);
    $group->post('/{id}/apply-coupon', [OrderController::class, 'applyCoupon']);
})->add(new AuthMiddleware());

// Admin routes (JWT auth + admin role check inside controller)
$app->get('/api/admin/users',          [AdminController::class, 'users']);
$app->post('/api/admin/users',         [AdminController::class, 'createUser']);
$app->delete('/api/admin/users/{id}',  [AdminController::class, 'deleteUser']);
$app->post('/api/admin/reset-db',      [AdminController::class, 'resetDb']);

// Log routes (JWT auth + admin role check inside controller)
$app->get('/api/logs',    [LogController::class, 'index']);
$app->delete('/api/logs', [LogController::class, 'clear']);

// Export routes (JWT auth inside controller)
$app->get('/api/export/{format}', [ExportController::class, 'export']);

$app->run();
