<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class OrderController
{
    public function index(Request $request, Response $response): Response
    {
        $claims = $request->getAttribute('jwt_claims');
        $db = Database::getInstance();

        if ($claims['role'] === 'admin') {
            $sqlQuery = "SELECT * FROM orders ORDER BY id DESC";
            $orders = $db->query($sqlQuery)->fetchAll();
        } else {
            $sqlQuery = "SELECT * FROM orders WHERE user_id = {$claims['sub']} ORDER BY id DESC";
            $stmt = $db->prepare("SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC");
            $stmt->execute([$claims['sub']]);
            $orders = $stmt->fetchAll();
        }

        Database::logRequest([
            'method' => 'GET', 'path' => '/api/orders',
            'queryParams' => '', 'body' => '',
            'statusCode' => 200, 'sqlQuery' => $sqlQuery,
            'responsePreview' => count($orders) . ' orders',
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'orders' => $orders,
            'count'  => count($orders),
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $id = $args['id'];
        $db = Database::getInstance();

        $sqlQuery = "SELECT o.*, u.nome as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = $id";

        $stmt = $db->prepare(
            "SELECT o.*, u.nome as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?"
        );
        $stmt->execute([$id]);
        $order = $stmt->fetch();

        Database::logRequest([
            'method' => 'GET', 'path' => "/api/orders/$id",
            'queryParams' => '', 'body' => '',
            'statusCode' => $order ? 200 : 404, 'sqlQuery' => $sqlQuery,
            'responsePreview' => $order ? "order {$order['id']}" : 'not found',
            'ip' => self::getIp($request),
        ]);

        if (!$order) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => 'Pedido nao encontrado',
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        $response->getBody()->write(json_encode($order));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function create(Request $request, Response $response): Response
    {
        $claims = $request->getAttribute('jwt_claims');

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

        $items = $body['items'] ?? [];
        $total = $body['total'] ?? 0;

        if (empty($items) || $total <= 0) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'Pedido deve conter itens e total valido',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $db = Database::getInstance();
        $stmt = $db->prepare("INSERT INTO orders (user_id, total, status) VALUES (?, ?, 'pendente')");
        $stmt->execute([$claims['sub'], $total]);

        $newOrderId = $db->lastInsertId();
        $stmt = $db->prepare("SELECT * FROM orders WHERE id = ?");
        $stmt->execute([$newOrderId]);
        $newOrder = $stmt->fetch();

        Database::logRequest([
            'method' => 'POST', 'path' => '/api/orders',
            'queryParams' => '', 'body' => json_encode($body),
            'statusCode' => 201,
            'sqlQuery' => "INSERT INTO orders (user_id, total, status) VALUES ({$claims['sub']}, $total, 'pendente')",
            'responsePreview' => "order $newOrderId created",
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message' => 'Pedido criado com sucesso',
            'order'   => $newOrder,
        ]));
        return $response->withStatus(201)->withHeader('Content-Type', 'application/json');
    }

    public function applyCoupon(Request $request, Response $response, array $args): Response
    {
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

        $code = $body['code'] ?? '';
        if (!$code) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'Codigo do cupom obrigatorio',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $db = Database::getInstance();

        $stmt = $db->prepare("SELECT * FROM coupons WHERE code = ? AND active = 1");
        $stmt->execute([$code]);
        $coupon = $stmt->fetch();

        if (!$coupon) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => 'Cupom nao encontrado',
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        if ($coupon['uses'] >= $coupon['max_uses']) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'Cupom esgotado',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        usleep(100000);

        $stmt = $db->prepare("UPDATE coupons SET uses = uses + 1 WHERE id = ?");
        $stmt->execute([$coupon['id']]);

        $stmt = $db->prepare("SELECT * FROM orders WHERE id = ?");
        $stmt->execute([$id]);
        $order = $stmt->fetch();

        if (!$order) {
            $response->getBody()->write(json_encode([
                'error' => 'Not Found',
                'message' => 'Pedido nao encontrado',
            ]));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        $discount = round($order['total'] * ($coupon['discount'] / 100) * 100) / 100;
        $newTotal = round(($order['total'] - $discount) * 100) / 100;

        $stmt = $db->prepare("UPDATE orders SET total = ? WHERE id = ?");
        $stmt->execute([$newTotal, $id]);

        Database::logRequest([
            'method' => 'POST', 'path' => "/api/orders/$id/apply-coupon",
            'queryParams' => '', 'body' => json_encode($body),
            'statusCode' => 200,
            'sqlQuery' => "UPDATE coupons SET uses = uses + 1 WHERE id = {$coupon['id']}; UPDATE orders SET total = $newTotal WHERE id = $id",
            'responsePreview' => "coupon $code applied, discount $discount",
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message'        => 'Cupom aplicado com sucesso',
            'discount'       => $discount,
            'original_total' => $order['total'],
            'new_total'      => $newTotal,
            'coupon_code'    => $code,
            'coupon_uses'    => $coupon['uses'] + 1,
            'coupon_max'     => $coupon['max_uses'],
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
