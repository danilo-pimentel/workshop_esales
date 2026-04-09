<?php

declare(strict_types=1);

namespace Tests;

use App\Controllers\ClienteController;
use App\Controllers\DashboardController;
use App\Controllers\PedidoController;
use App\Controllers\ProdutoController;
use App\Database;
use App\Middleware\CorsMiddleware;
use PDO;
use PHPUnit\Framework\TestCase;
use Slim\Factory\AppFactory;
use Slim\Psr7\Factory\ServerRequestFactory;
use Slim\Psr7\Factory\StreamFactory;

class ApiTest extends TestCase
{
    private static PDO $db;
    private \Slim\App $app;

    public static function setUpBeforeClass(): void
    {
        self::$db = Database::createTestInstance();
    }

    protected function setUp(): void
    {
        $db = self::$db;

        $app = AppFactory::create();
        $app->addBodyParsingMiddleware();
        $app->addRoutingMiddleware();
        $app->addErrorMiddleware(false, false, false);
        $app->add(CorsMiddleware::class);

        $app->options('/{routes:.+}', function ($request, $response) {
            return $response;
        });

        // Produtos
        $app->get('/api/produtos', function ($req, $res) use ($db) {
            return (new ProdutoController($db))->index($req, $res);
        });
        $app->get('/api/produtos/{id:[0-9]+}', function ($req, $res, $args) use ($db) {
            return (new ProdutoController($db))->show($req, $res, $args);
        });
        $app->post('/api/produtos', function ($req, $res) use ($db) {
            return (new ProdutoController($db))->create($req, $res);
        });
        $app->put('/api/produtos/{id:[0-9]+}', function ($req, $res, $args) use ($db) {
            return (new ProdutoController($db))->update($req, $res, $args);
        });
        $app->delete('/api/produtos/{id:[0-9]+}', function ($req, $res, $args) use ($db) {
            return (new ProdutoController($db))->delete($req, $res, $args);
        });

        // Pedidos
        $app->get('/api/pedidos', function ($req, $res) use ($db) {
            return (new PedidoController($db))->index($req, $res);
        });
        $app->get('/api/pedidos/{id:[0-9]+}', function ($req, $res, $args) use ($db) {
            return (new PedidoController($db))->show($req, $res, $args);
        });
        $app->post('/api/pedidos', function ($req, $res) use ($db) {
            return (new PedidoController($db))->create($req, $res);
        });
        $app->patch('/api/pedidos/{id:[0-9]+}/status', function ($req, $res, $args) use ($db) {
            return (new PedidoController($db))->updateStatus($req, $res, $args);
        });

        // Clientes
        $app->get('/api/clientes', function ($req, $res) use ($db) {
            return (new ClienteController($db))->index($req, $res);
        });
        $app->get('/api/clientes/{id:[0-9]+}', function ($req, $res, $args) use ($db) {
            return (new ClienteController($db))->show($req, $res, $args);
        });

        // Dashboard
        $app->get('/api/dashboard/resumo', function ($req, $res) use ($db) {
            return (new DashboardController($db))->resumo($req, $res);
        });

        $this->app = $app;
    }

    private function request(string $method, string $uri, ?array $body = null): array
    {
        $factory = new ServerRequestFactory();
        $request = $factory->createServerRequest($method, $uri);

        if ($body !== null) {
            $streamFactory = new StreamFactory();
            $json = json_encode($body);
            $stream = $streamFactory->createStream($json);
            $request = $request
                ->withHeader('Content-Type', 'application/json')
                ->withBody($stream)
                ->withParsedBody($body);
        }

        $response = $this->app->handle($request);
        $status = $response->getStatusCode();
        $bodyStr = (string)$response->getBody();
        $data = json_decode($bodyStr, true);

        return [$status, $data];
    }

    public function testCrudProduto(): void
    {
        // CREATE
        [$status, $data] = $this->request('POST', '/api/produtos', [
            'nome'      => 'Produto Teste CRUD',
            'descricao' => 'Descricao do produto de teste',
            'preco'     => 99.90,
            'estoque'   => 10,
            'categoria' => 'Teste',
        ]);
        $this->assertSame(201, $status, 'POST /api/produtos deve retornar 201');
        $this->assertArrayHasKey('id', $data);
        $this->assertSame('Produto Teste CRUD', $data['nome']);
        $this->assertSame(99.90, (float)$data['preco']);
        $id = (int)$data['id'];

        // READ
        [$status, $data] = $this->request('GET', "/api/produtos/{$id}");
        $this->assertSame(200, $status);
        $this->assertSame('Produto Teste CRUD', $data['nome']);

        // UPDATE
        [$status, $data] = $this->request('PUT', "/api/produtos/{$id}", [
            'nome'    => 'Produto Atualizado',
            'preco'   => 149.90,
            'estoque' => 5,
        ]);
        $this->assertSame(200, $status);
        $this->assertSame('Produto Atualizado', $data['nome']);
        $this->assertSame(149.90, (float)$data['preco']);

        // DELETE
        [$status, $data] = $this->request('DELETE', "/api/produtos/{$id}");
        $this->assertSame(204, $status);

        // VERIFY DELETED
        [$status,] = $this->request('GET', "/api/produtos/{$id}");
        $this->assertSame(404, $status);
    }

    public function testBuscaCaseInsensitive(): void
    {
        [$status, $data] = $this->request('GET', '/api/produtos?busca=notebook');

        $this->assertSame(200, $status);
        $this->assertGreaterThan(
            0,
            $data['total'],
            'Busca por "notebook" deveria encontrar "Notebook Pro"'
        );
    }

    public function testCriarPedido(): void
    {
        [$status, $data] = $this->request('POST', '/api/pedidos', [
            'cliente_id' => 1,
            'itens'      => [
                ['produto_id' => 1, 'quantidade' => 2],
                ['produto_id' => 2, 'quantidade' => 1],
            ],
        ]);

        $this->assertSame(201, $status, 'POST /api/pedidos deve retornar 201');
        $this->assertArrayHasKey('id', $data);
        $this->assertSame(1, (int)$data['cliente_id']);
        $this->assertSame('pendente', $data['status']);

        $pedidoId = (int)$data['id'];
        [$status, $pedido] = $this->request('GET', "/api/pedidos/{$pedidoId}");
        $this->assertSame(200, $status);
        $this->assertSame($pedidoId, (int)$pedido['id']);
        $this->assertArrayHasKey('itens', $pedido);
        $this->assertArrayHasKey('cliente_nome', $pedido);
    }

    public function testPrecoNegativoRejeitado(): void
    {
        [$status, $data] = $this->request('POST', '/api/produtos', [
            'nome'  => 'Produto Preco Negativo',
            'preco' => -10.00,
        ]);

        $this->assertSame(
            400,
            $status,
            'Preco negativo deve ser rejeitado com status 400'
        );
    }

    public function testListarClientes(): void
    {
        [$status, $data] = $this->request('GET', '/api/clientes');

        $this->assertSame(200, $status, 'GET /api/clientes deve retornar 200');
        $this->assertIsArray($data);
        $this->assertCount(20, $data, 'Deve haver 20 clientes no seed');

        [$status, $cliente] = $this->request('GET', '/api/clientes/1');
        $this->assertSame(200, $status);
        $this->assertArrayHasKey('pedidos', $cliente);
        $this->assertArrayHasKey('total_pedidos', $cliente);
        $this->assertArrayHasKey('total_gasto', $cliente);
        $this->assertSame('Ana Paula Ferreira', $cliente['nome']);
    }

    public function testPaginacaoTotalPages(): void
    {
        [$status, $data] = $this->request('GET', '/api/produtos?page=1&limit=7');

        $this->assertSame(200, $status);
        $total = (int)$data['total'];
        $limit = (int)$data['limit'];
        $totalPages = (int)$data['totalPages'];

        $expectedPages = (int)ceil($total / $limit);
        $this->assertSame(
            $expectedPages,
            $totalPages,
            "totalPages deve ser ceil({$total}/{$limit})={$expectedPages}, mas foi {$totalPages}"
        );
    }

    public function testDashboardResumo(): void
    {
        [$status, $data] = $this->request('GET', '/api/dashboard/resumo');

        $this->assertSame(200, $status, 'GET /api/dashboard/resumo deve retornar 200');
        $this->assertArrayHasKey('total_vendas', $data);
        $this->assertArrayHasKey('total_pedidos', $data);
        $this->assertArrayHasKey('ticket_medio', $data);

        $this->assertGreaterThan(0, $data['total_vendas'], 'total_vendas deve ser positivo');
        $this->assertGreaterThan(0, $data['total_pedidos'], 'total_pedidos deve ser positivo');
        $this->assertGreaterThan(0, $data['ticket_medio'], 'ticket_medio deve ser positivo');

        $expectedTicket = round($data['total_vendas'] / $data['total_pedidos'], 2);
        $this->assertEqualsWithDelta($expectedTicket, $data['ticket_medio'], 0.01);
    }

    public function testTotalPedidoComQuantidade(): void
    {
        [$status, $produto] = $this->request('POST', '/api/produtos', [
            'nome'      => 'Produto Para Teste Quantidade',
            'descricao' => 'Produto de teste',
            'preco'     => 100.00,
            'estoque'   => 50,
            'categoria' => 'Teste',
        ]);
        $this->assertSame(201, $status);
        $produtoId = (int)$produto['id'];

        [$status, $pedido] = $this->request('POST', '/api/pedidos', [
            'cliente_id' => 1,
            'itens'      => [
                ['produto_id' => $produtoId, 'quantidade' => 3],
            ],
        ]);

        $this->assertSame(201, $status);
        $this->assertEqualsWithDelta(
            300.00,
            (float)$pedido['total'],
            0.01,
            "Total do pedido deveria ser 300.00 (3 x 100.00), mas foi {$pedido['total']}"
        );
    }

    public function testFiltroStatus(): void
    {
        [$status, $todos] = $this->request('GET', '/api/pedidos?limit=200');
        $this->assertSame(200, $status);

        [$status, $entregues] = $this->request('GET', '/api/pedidos?status=entregue&limit=200');
        $this->assertSame(200, $status);

        foreach ($entregues['data'] as $pedido) {
            $this->assertSame(
                'entregue',
                $pedido['status'],
                'Filtro por status=entregue retornou pedido com status diferente'
            );
        }

        $this->assertLessThan(
            (int)$todos['total'],
            (int)$entregues['total'],
            'Filtro por status deveria retornar menos pedidos que o total'
        );
    }
}
