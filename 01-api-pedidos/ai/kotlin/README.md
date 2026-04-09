# API de Gerenciamento de Pedidos

REST API completa para um sistema de e-commerce, construída com **Kotlin + Ktor + SQLite**.

## Pré-requisitos

- **JDK 21** ou superior
- **Gradle 8+** (ou use o wrapper `./gradlew`)

## Executar a aplicação

```bash
# Linux / macOS:
./gradlew run

# Windows (PowerShell):
.\gradlew run
```

A API ficará disponível em `http://localhost:3000`.

## Executar os testes

```bash
# Linux / macOS:
./gradlew test

# Windows (PowerShell):
.\gradlew test
```

Os relatórios de teste ficam em `build/reports/tests/test/index.html`.

## Estrutura do projeto

```
src/
├── main/kotlin/com/treinamento/
│   ├── Application.kt          # Ponto de entrada, configuração do servidor
│   ├── models/Models.kt        # Tabelas Exposed + data classes
│   ├── database/Database.kt    # Inicialização do banco e seed de dados
│   └── routes/
│       ├── ProdutoRoutes.kt
│       ├── PedidoRoutes.kt
│       ├── ClienteRoutes.kt
│       └── DashboardRoutes.kt
└── test/kotlin/com/treinamento/
    └── ApiTest.kt
```

## Endpoints

### Produtos

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/produtos` | Listar produtos com paginação e busca |
| `GET` | `/api/produtos/{id}` | Buscar produto por ID |
| `POST` | `/api/produtos` | Criar novo produto |
| `PUT` | `/api/produtos/{id}` | Atualizar produto |
| `DELETE` | `/api/produtos/{id}` | Remover produto |

**Query params (GET /api/produtos):** `busca`, `page` (default 1), `limit` (default 10)

### Pedidos

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/pedidos` | Listar pedidos com paginação |
| `GET` | `/api/pedidos/{id}` | Buscar pedido por ID (com itens e cliente) |
| `POST` | `/api/pedidos` | Criar novo pedido |
| `PATCH` | `/api/pedidos/{id}/status` | Atualizar status do pedido |

**Status válidos:** `pendente`, `processando`, `enviado`, `entregue`, `cancelado`

### Clientes

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/clientes` | Listar todos os clientes |
| `GET` | `/api/clientes/{id}` | Buscar cliente por ID (com pedidos) |

### Dashboard

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/dashboard/resumo` | Total de vendas, pedidos e ticket médio |

## Exemplos de uso

### Criar produto

```http
POST /api/produtos
Content-Type: application/json

{
  "nome": "Smartphone XYZ",
  "descricao": "Smartphone de última geração",
  "preco": 1999.90,
  "estoque": 50,
  "categoria": "Eletrônicos"
}
```

### Criar pedido

```http
POST /api/pedidos
Content-Type: application/json

{
  "clienteId": 1,
  "itens": [
    { "produtoId": 1, "quantidade": 2 },
    { "produtoId": 3, "quantidade": 1 }
  ]
}
```

### Atualizar status do pedido

```http
PATCH /api/pedidos/1/status
Content-Type: application/json

{ "status": "enviado" }
```

## Dados de exemplo

O banco é inicializado automaticamente com:

- **50 produtos** nas categorias: Eletrônicos, Roupas, Alimentos, Livros, Esportes
- **20 clientes** com nomes brasileiros
- **100 pedidos** distribuídos ao longo de 2024
- **200+ itens de pedido**

## Tecnologias

- **Kotlin 1.9.22**
- **Ktor 2.3.7** — framework web
- **Exposed 0.45.0** — ORM
- **SQLite 3.44.1.0** — banco de dados
- **kotlinx.serialization** — serialização JSON
- **kotlin.test + Ktor testApplication** — testes
