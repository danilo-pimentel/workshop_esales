# API de Pedidos — PHP 8.3 + Slim Framework 4

REST API para gerenciamento de pedidos de e-commerce, construída com PHP 8.3, Slim Framework 4 e PDO SQLite em memória.

## Pré-requisitos

- PHP 8.3+
- Composer

## Instalação e execução

```bash
composer install
php -S localhost:3000 -t public
```

A API estará disponível em `http://localhost:3000`.

## Testes

```bash
# Linux / macOS:
./vendor/bin/phpunit

# Windows (PowerShell):
.\vendor\bin\phpunit
```

## Endpoints

### Produtos

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/produtos` | Listar produtos (suporta `busca`, `page`, `limit`) |
| GET | `/api/produtos/{id}` | Buscar produto por ID |
| POST | `/api/produtos` | Criar produto |
| PUT | `/api/produtos/{id}` | Atualizar produto |
| DELETE | `/api/produtos/{id}` | Remover produto |

**Exemplo de criação:**
```json
POST /api/produtos
{
  "nome": "Produto Exemplo",
  "descricao": "Descrição do produto",
  "preco": 99.90,
  "estoque": 100,
  "categoria": "Eletrônicos"
}
```

**Resposta paginada:**
```json
{
  "data": [...],
  "page": 1,
  "limit": 10,
  "total": 50,
  "totalPages": 5
}
```

### Pedidos

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/pedidos` | Listar pedidos (suporta `page`, `limit`) |
| GET | `/api/pedidos/{id}` | Buscar pedido com itens e cliente |
| POST | `/api/pedidos` | Criar pedido |
| PATCH | `/api/pedidos/{id}/status` | Atualizar status do pedido |

**Exemplo de criação:**
```json
POST /api/pedidos
{
  "cliente_id": 1,
  "itens": [
    { "produto_id": 1, "quantidade": 2 },
    { "produto_id": 5, "quantidade": 1 }
  ]
}
```

**Status válidos:** `pendente`, `confirmado`, `enviado`, `entregue`, `cancelado`

### Clientes

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/clientes` | Listar clientes |
| GET | `/api/clientes/{id}` | Buscar cliente com histórico de pedidos |

### Dashboard

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/dashboard/resumo` | Resumo de vendas, pedidos e ticket médio |

**Exemplo de resposta:**
```json
{
  "total_vendas": 125000.00,
  "total_pedidos": 80,
  "ticket_medio": 1562.50,
  "por_status": [...],
  "top_produtos": [...],
  "top_clientes": [...],
  "vendas_por_mes": [...]
}
```

## Tecnologias

- **PHP 8.3** — linguagem principal
- **Slim Framework 4** — microframework HTTP
- **PDO SQLite** — banco de dados em memória
- **PHPUnit 10** — testes automatizados
- **CORS** — habilitado para todos os origens

## Estrutura do projeto

```
php/
├── composer.json
├── phpunit.xml
├── public/
│   └── index.php          # Bootstrap da aplicação e rotas
├── src/
│   ├── Database.php       # Conexão PDO, schema e seed
│   ├── Controllers/
│   │   ├── ProdutoController.php
│   │   ├── PedidoController.php
│   │   ├── ClienteController.php
│   │   └── DashboardController.php
│   └── Middleware/
│       └── CorsMiddleware.php
└── tests/
    └── ApiTest.php
```
