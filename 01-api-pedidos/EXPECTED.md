# Expected Behavior — Ex.01 API de Pedidos

Este documento descreve o comportamento **esperado** da API, organizado pelos 5 testes automatizados que falham. Use-o como referência para validar suas correções.

> A API roda em `http://localhost:3000`

---

## Indice

| # | Teste | HTTP | Endpoint |
|---|-------|:---:|----------|
| 1 | [testBuscaCaseInsensitive](#1-testbuscacaseinsensitive) | `GET` | `/api/produtos?busca=` |
| 2 | [testPrecoNegativoRejeitado](#2-testpreconegativorejeitado) | `POST` | `/api/produtos` |
| 3 | [testPaginacaoTotalPages](#3-testpaginacaototalpages) | `GET` | `/api/produtos?page=&limit=` |
| 4 | [testTotalPedidoComQuantidade](#4-testtotalpedidocomquantidade) | `POST` | `/api/pedidos` |
| 5 | [testFiltroStatus](#5-testfiltrostatus) | `GET` | `/api/pedidos?status=` |

---

## 1) testBuscaCaseInsensitive

**Descricao:** A busca de produtos deve ser case-insensitive. Buscar "notebook" (minusculo) deve encontrar o produto "Notebook Pro".

### Endpoint

```http
GET /api/produtos?busca=notebook
```

### Como testar

```bash
curl "http://localhost:3000/api/produtos?busca=notebook"
```

### Comportamento atual (bugado)

```json
{
  "data": [],
  "total": 0,
  "page": 1,
  "limit": 10,
  "totalPages": 0
}
```

### Comportamento esperado

```json
{
  "data": [
    {
      "id": 1,
      "nome": "Notebook Pro",
      "descricao": "...",
      "preco": 4500.00,
      "estoque": 15,
      "categoria": "Eletronicos"
    }
  ],
  "total": 1,
  "page": 1,
  "limit": 10,
  "totalPages": 1
}
```

### Teste de comparacao (funciona)

```bash
curl "http://localhost:3000/api/produtos?busca=Notebook"   # N maiusculo funciona
```

---

## 2) testPrecoNegativoRejeitado

**Descricao:** A API deve rejeitar requisicoes de criacao de produto com preco negativo, retornando HTTP `400 Bad Request`.

### Endpoint

```http
POST /api/produtos
Content-Type: application/json
```

### Payload

```json
{
  "nome": "Produto Preco Negativo",
  "preco": -10.00,
  "estoque": 5,
  "categoria": "Teste"
}
```

### Como testar

```bash
curl -X POST http://localhost:3000/api/produtos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Produto Preco Negativo",
    "preco": -10.00,
    "estoque": 5,
    "categoria": "Teste"
  }'
```

### Comportamento atual (bugado)

**Status:** `201 Created` — aceita o preco negativo!

```json
{
  "id": 52,
  "nome": "Produto Preco Negativo",
  "preco": -10,
  "estoque": 5,
  "categoria": "Teste"
}
```

### Comportamento esperado

**Status:** `400 Bad Request`

```json
{
  "error": "Preco nao pode ser negativo"
}
```

---

## 3) testPaginacaoTotalPages

**Descricao:** O calculo de `totalPages` deve usar divisao com teto (ceil) para nao perder a ultima pagina em casos de divisao nao exata.

### Endpoint

```http
GET /api/produtos?page=1&limit=7
```

### Como testar

```bash
curl "http://localhost:3000/api/produtos?page=1&limit=7"
```

### Comportamento atual (bugado)

Com **51 produtos** no seed e `limit=7`, `totalPages` retorna **7** (floor).

```json
{
  "data": [ ... 7 produtos ... ],
  "total": 51,
  "page": 1,
  "limit": 7,
  "totalPages": 7
}
```

### Comportamento esperado

`totalPages` deve ser **8**, pois `ceil(51 / 7) = 8`.

```json
{
  "data": [ ... 7 produtos ... ],
  "total": 51,
  "page": 1,
  "limit": 7,
  "totalPages": 8
}
```

### Verificacao visual

```bash
# Acessar a pagina 8 (deveria existir, retorna 2 produtos)
curl "http://localhost:3000/api/produtos?page=8&limit=7"
```

Atualmente a API retorna dados na pagina 8, mas `totalPages` esta dizendo que ela nao existe.

---

## 4) testTotalPedidoComQuantidade

**Descricao:** Ao criar um pedido, o total deve ser calculado como **preco unitario x quantidade**. O bug atual ignora a quantidade.

### Passo 1 — Criar produto de teste

```http
POST /api/produtos
Content-Type: application/json
```

```json
{
  "nome": "Produto Para Teste Quantidade",
  "descricao": "Produto de teste",
  "preco": 100.00,
  "estoque": 50,
  "categoria": "Teste"
}
```

```bash
curl -X POST http://localhost:3000/api/produtos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Produto Teste",
    "preco": 100.00,
    "estoque": 50,
    "categoria": "Teste"
  }'
```

> Anote o `id` retornado, por exemplo: `52`

### Passo 2 — Criar pedido com quantidade 3

```http
POST /api/pedidos
Content-Type: application/json
```

```json
{
  "cliente_id": 1,
  "itens": [
    { "produto_id": 52, "quantidade": 3 }
  ]
}
```

```bash
curl -X POST http://localhost:3000/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "cliente_id": 1,
    "itens": [{"produto_id": 52, "quantidade": 3}]
  }'
```

### Comportamento atual (bugado)

```json
{
  "id": 101,
  "cliente_id": 1,
  "status": "pendente",
  "total": 100.00
}
```

> Total esta errado: soma apenas o preco unitario uma vez.

### Comportamento esperado

```json
{
  "id": 101,
  "cliente_id": 1,
  "status": "pendente",
  "total": 300.00
}
```

> 3 unidades x R$ 100,00 = **R$ 300,00**

---

## 5) testFiltroStatus

**Descricao:** O endpoint de listar pedidos deve aceitar filtros por `status`, `data_inicio` e `data_fim`. Atualmente os parametros sao aceitos mas ignorados.

### Endpoint

```http
GET /api/pedidos?status=entregue&limit=200
```

### Como testar

```bash
# Listar TODOS os pedidos (baseline: 100 pedidos no seed)
curl "http://localhost:3000/api/pedidos?limit=200"

# Filtrar apenas os pedidos com status "entregue"
curl "http://localhost:3000/api/pedidos?status=entregue&limit=200"
```

### Comportamento atual (bugado)

Retorna TODOS os 100 pedidos, ignorando o filtro:

```json
{
  "data": [
    { "id": 1, "status": "pendente", ... },
    { "id": 2, "status": "processando", ... },
    { "id": 3, "status": "entregue", ... },
    { "id": 4, "status": "enviado", ... }
  ],
  "total": 100,
  "page": 1,
  "limit": 200
}
```

### Comportamento esperado

Retorna apenas pedidos com `status = "entregue"`:

```json
{
  "data": [
    { "id": 3, "status": "entregue", ... },
    { "id": 7, "status": "entregue", ... },
    { "id": 12, "status": "entregue", ... }
  ],
  "total": 20,
  "page": 1,
  "limit": 200
}
```

### Testes adicionais da feature

```bash
# Filtro por intervalo de data
curl "http://localhost:3000/api/pedidos?data_inicio=2024-01-01&data_fim=2024-06-30"

# Combinacao de filtros
curl "http://localhost:3000/api/pedidos?status=entregue&data_inicio=2024-01-01&data_fim=2024-12-31"

# Status + paginacao
curl "http://localhost:3000/api/pedidos?status=pendente&page=1&limit=10"
```

---

## Status validos para pedidos

Ao testar o filtro por status, use um destes valores:

| Status | Descricao |
|--------|-----------|
| `pendente` | Pedido criado, aguardando processamento |
| `processando` | Em processamento |
| `enviado` | Despachado |
| `entregue` | Entregue ao cliente |
| `cancelado` | Cancelado |

---

## Script de validacao rapida

Salve como `test-expected.sh` e rode apos subir a API:

```bash
#!/bin/bash
API="http://localhost:3000"

echo "================================================================"
echo "  TESTES DE COMPORTAMENTO ESPERADO — Ex.01 API de Pedidos"
echo "================================================================"

echo ""
echo "1) Busca case-insensitive: buscar 'notebook' deve encontrar 'Notebook Pro'"
TOTAL=$(curl -s "$API/api/produtos?busca=notebook" | grep -oE '"total":[0-9]+' | cut -d: -f2)
echo "   total encontrado: $TOTAL (esperado: >= 1)"

echo ""
echo "2) Preco negativo: deve retornar HTTP 400"
STATUS=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$API/api/produtos" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teste","preco":-10,"estoque":5,"categoria":"T"}')
echo "   status retornado: $STATUS (esperado: 400)"

echo ""
echo "3) Paginacao: 51 itens / limit 7 deve ter 8 paginas"
TP=$(curl -s "$API/api/produtos?page=1&limit=7" | grep -oE '"totalPages":[0-9]+' | cut -d: -f2)
echo "   totalPages: $TP (esperado: 8)"

echo ""
echo "4) Total com quantidade: 3 x R\$100 deve ser R\$300"
PROD_ID=$(curl -s -X POST "$API/api/produtos" \
  -H "Content-Type: application/json" \
  -d '{"nome":"TesteQtd","preco":100,"estoque":50,"categoria":"T"}' \
  | grep -oE '"id":[0-9]+' | head -1 | cut -d: -f2)
TOTAL=$(curl -s -X POST "$API/api/pedidos" \
  -H "Content-Type: application/json" \
  -d "{\"cliente_id\":1,\"itens\":[{\"produto_id\":$PROD_ID,\"quantidade\":3}]}" \
  | grep -oE '"total":[0-9.]+' | cut -d: -f2)
echo "   total calculado: $TOTAL (esperado: 300)"

echo ""
echo "5) Filtro por status: deve retornar apenas pedidos com status=entregue"
curl -s "$API/api/pedidos?status=entregue&limit=200" \
  | grep -oE '"status":"[a-z]+"' | sort -u
echo "   esperado: apenas \"entregue\""

echo ""
echo "================================================================"
```

```bash
chmod +x test-expected.sh
./test-expected.sh
```

---

## Resumo dos endpoints da API

Para referencia, estes sao todos os endpoints disponiveis:

### Produtos
```
GET    /api/produtos?busca=&page=&limit=    Listar produtos (paginado, com busca)
GET    /api/produtos/:id                     Detalhe do produto
POST   /api/produtos                         Criar produto
PUT    /api/produtos/:id                     Atualizar produto
DELETE /api/produtos/:id                     Remover produto
```

### Pedidos
```
GET    /api/pedidos?page=&limit=             Listar pedidos (paginado)
GET    /api/pedidos/:id                      Detalhe do pedido com itens
POST   /api/pedidos                          Criar pedido
PATCH  /api/pedidos/:id/status               Atualizar status do pedido
```

### Clientes
```
GET    /api/clientes                         Listar clientes
GET    /api/clientes/:id                     Detalhe com historico de pedidos
```

### Dashboard
```
GET    /api/dashboard/resumo                 Resumo: vendas, pedidos, ticket medio
```
