# Exercicio 01 - API de Gestao de Pedidos

## Contexto

Voce recebeu uma API REST de gestao de pedidos de um e-commerce. A API gerencia produtos, clientes e pedidos, e expoe endpoints para um dashboard frontend.

A API esta funcional, mas **possui bugs** que precisam ser corrigidos e **uma feature** que precisa ser implementada.

---

## Dinamica: 2 Fases

Este exercicio sera executado **duas vezes**, com abordagens diferentes:

### Fase 1: Resolucao Manual (`manual/`)

Resolva os bugs e implemente a feature **sem usar nenhuma ferramenta de AI**.
Use apenas a IDE, documentacao oficial, Stack Overflow, e seu conhecimento.

> **Objetivo:** Estabelecer uma baseline de tempo e esforco para comparacao.

### Fase 2: Resolucao com AI (`ai/`)

Resolva os mesmos bugs e implemente a mesma feature **usando ferramentas de AI** (Claude Code, Copilot, ChatGPT, etc).

> **Objetivo:** Comparar velocidade, qualidade e experiencia com vs sem AI.

**O codigo nas duas pastas e identico.** A unica diferenca e a abordagem usada.

---

## Escolha sua linguagem

| Stack | Pasta | Como rodar (Linux/macOS) | Como rodar (Windows PowerShell) |
|-------|-------|-----------|-----------|
| Java (Spring Boot) | `java/` | `mvn spring-boot:run` | `mvn spring-boot:run` |
| Kotlin (Ktor) | `kotlin/` | `./gradlew run` | `.\gradlew run` |
| PHP (Slim) | `php/` | `composer install && php -S localhost:3000 -t public` | `composer install; php -S localhost:3000 -t public` |
| Bun (Elysia) | `bun/` | `bun install && bun run src/index.ts` | `bun install; bun run src/index.ts` |

Todas as implementacoes tem os mesmos endpoints, mesmos bugs e mesma feature faltando.

**Importante:** Navegue ate a pasta da fase antes de escolher a linguagem:
```bash
# Fase 1 (manual)
cd manual/bun    # ou manual/java, manual/kotlin, manual/php

# Fase 2 (AI)
cd ai/bun        # ou ai/java, ai/kotlin, ai/php
```

## Endpoints

```
GET    /api/produtos?busca=X&page=1&limit=10   Listar produtos (paginado, com busca)
GET    /api/produtos/:id                        Detalhe do produto
POST   /api/produtos                            Criar produto
PUT    /api/produtos/:id                        Atualizar produto
DELETE /api/produtos/:id                        Remover produto

GET    /api/pedidos?page=1&limit=10             Listar pedidos (paginado)
GET    /api/pedidos/:id                         Detalhe do pedido com itens
POST   /api/pedidos                             Criar pedido
PATCH  /api/pedidos/:id/status                  Atualizar status

GET    /api/clientes                            Listar clientes
GET    /api/clientes/:id                        Detalhe com historico

GET    /api/dashboard/resumo                    Resumo: vendas, pedidos, ticket medio
```

## O que voce precisa fazer

### Corrigir bugs

Os **testes automatizados** indicam os problemas. Execute os testes da sua linguagem e corrija o que estiver falhando.

### Implementar feature

A API precisa suportar **filtros no endpoint de pedidos**:

```
GET /api/pedidos?status=pendente&data_inicio=2024-01-01&data_fim=2024-06-30
```

Atualmente esses parametros sao ignorados.

## Como rodar os testes

```bash
# Bun
bun install && bun test

# Java
mvn test

# Kotlin
./gradlew test    # Linux
.\gradlew test    # Windows

# PHP
composer install && ./vendor/bin/phpunit
```

## Criterios de aceite

- Todos os testes automatizados passam
- O filtro por status e data funciona corretamente
- A API continua funcionando em http://localhost:3000

## Tempo estimado

| Fase | Tempo |
|------|-------|
| Manual (sem AI) | 20-30 min |
| Com AI | 5-15 min |

Anote o tempo gasto em cada fase para comparacao!
