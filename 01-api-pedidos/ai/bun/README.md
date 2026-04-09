# Ex.01 - API de Pedidos (Bun + Elysia)

## Pré-requisitos
- Bun 1.1+

## Como executar

```bash
bun install
bun run src/index.ts
```

API disponível em http://localhost:3000

## Como testar

```bash
bun test
```

## Endpoints

### Produtos
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | /api/produtos | Lista produtos (suporta `?busca=X&page=1&limit=10`) |
| GET | /api/produtos/:id | Busca produto por ID |
| POST | /api/produtos | Cria novo produto |
| PUT | /api/produtos/:id | Atualiza produto |
| DELETE | /api/produtos/:id | Remove produto |

### Pedidos
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | /api/pedidos | Lista pedidos paginados (`?page=1&limit=10`) |
| GET | /api/pedidos/:id | Busca pedido com itens e dados do cliente |
| POST | /api/pedidos | Cria pedido com itens |
| PATCH | /api/pedidos/:id/status | Atualiza status do pedido |

### Clientes
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | /api/clientes | Lista todos os clientes |
| GET | /api/clientes/:id | Busca cliente com histórico de pedidos |

### Dashboard
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | /api/dashboard/resumo | Resumo geral: total de vendas, pedidos e ticket médio |

## Modelos de dados

### POST /api/produtos
```json
{
  "nome": "string (obrigatório)",
  "descricao": "string (opcional)",
  "preco": 0.0,
  "estoque": 0,
  "categoria": "string (opcional)"
}
```

### POST /api/pedidos
```json
{
  "cliente_id": 1,
  "itens": [
    { "produto_id": 1, "quantidade": 2 }
  ]
}
```

### PATCH /api/pedidos/:id/status
```json
{
  "status": "pendente | processando | enviado | entregue | cancelado"
}
```

## Respostas de listagem paginada

```json
{
  "data": [...],
  "page": 1,
  "limit": 10,
  "total": 50,
  "totalPages": 5
}
```
