# API de Pedidos - Sistema de Gerenciamento de E-commerce

API REST para gerenciamento de pedidos, produtos e clientes de um sistema de e-commerce, construída com Java 21 + Spring Boot 3.2 + H2 Database.

## Pré-requisitos

- **JDK 21** ou superior
- **Apache Maven 3.8+**

## Como Executar

```bash
cd exercicios/01-api-pedidos/java
mvn spring-boot:run
```

A API estará disponível em: `http://localhost:3000`

## Console H2

Acesse o banco de dados em memória pelo console web:

- URL: `http://localhost:3000/h2-console`
- JDBC URL: `jdbc:h2:mem:pedidos`
- Username: `sa`
- Password: (deixe em branco)

## Executar Testes

```bash
mvn test
```

## Endpoints Disponíveis

### Produtos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/produtos` | Listar produtos com paginação (`?busca=X&page=1&limit=10`) |
| GET | `/api/produtos/{id}` | Buscar produto por ID |
| POST | `/api/produtos` | Criar novo produto |
| PUT | `/api/produtos/{id}` | Atualizar produto |
| DELETE | `/api/produtos/{id}` | Remover produto |

### Pedidos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/pedidos` | Listar pedidos com paginação (`?page=1&limit=10`) |
| GET | `/api/pedidos/{id}` | Buscar pedido por ID (com itens e cliente) |
| POST | `/api/pedidos` | Criar novo pedido |
| PATCH | `/api/pedidos/{id}/status` | Atualizar status do pedido |

### Clientes

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/clientes` | Listar todos os clientes |
| GET | `/api/clientes/{id}` | Buscar cliente por ID (com histórico de pedidos) |

### Dashboard

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/dashboard/resumo` | Resumo com totalVendas, totalPedidos e ticketMedio |

## Exemplos de Uso

### Criar produto

```bash
# Linux / macOS:
curl -X POST http://localhost:3000/api/produtos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Notebook Gamer","descricao":"Alta performance","preco":5999.90,"estoque":10,"categoria":"Eletrônicos"}'

# Windows (PowerShell):
curl -X POST http://localhost:3000/api/produtos `
  -H "Content-Type: application/json" `
  -d '{"nome":"Notebook Gamer","descricao":"Alta performance","preco":5999.90,"estoque":10,"categoria":"Eletrônicos"}'
```

### Criar pedido

```bash
# Linux / macOS:
curl -X POST http://localhost:3000/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":1,"itens":[{"produtoId":1,"quantidade":1},{"produtoId":3,"quantidade":2}]}'

# Windows (PowerShell):
curl -X POST http://localhost:3000/api/pedidos `
  -H "Content-Type: application/json" `
  -d '{"clienteId":1,"itens":[{"produtoId":1,"quantidade":1},{"produtoId":3,"quantidade":2}]}'
```

### Atualizar status do pedido

```bash
# Linux / macOS:
curl -X PATCH http://localhost:3000/api/pedidos/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"enviado"}'

# Windows (PowerShell):
curl -X PATCH http://localhost:3000/api/pedidos/1/status `
  -H "Content-Type: application/json" `
  -d '{"status":"enviado"}'
```

### Buscar produtos com paginação

```bash
curl "http://localhost:3000/api/produtos?busca=notebook&page=1&limit=10"
```

## Estrutura de Resposta Paginada

```json
{
  "data": [...],
  "page": 1,
  "limit": 10,
  "total": 50,
  "totalPages": 5
}
```

## Tecnologias

- Java 21
- Spring Boot 3.2
- Spring Data JPA
- H2 Database (in-memory)
- JUnit 5 + MockMvc
