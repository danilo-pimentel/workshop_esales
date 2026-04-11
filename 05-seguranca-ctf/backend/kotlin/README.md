# CTF Backend — Kotlin / Ktor

**AVISO: Esta API contém vulnerabilidades DELIBERADAS para fins educacionais de treinamento em segurança (CTF). NÃO implante em produção.**

---

## Início Rápido

```bash
# Executar a API (porta 4000)
./gradlew run

# Executar os testes de vulnerabilidades
./gradlew test

# Gerar fat-jar
./gradlew buildFatJar
java -jar build/libs/ctf-api-all.jar
```

**JDK 17+ obrigatório.** O banco SQLite (`ctf.db`) é criado automaticamente na primeira execução.

---

## Stack

| Componente        | Versão     |
|-------------------|------------|
| Kotlin            | 1.9.23     |
| Ktor (Netty)      | 2.3.10     |
| SQLite (JDBC raw) | 3.45.3.0   |
| kotlinx.serialization | 1.6.3  |
| com.auth0:java-jwt | 4.4.0     |
| JUnit 5           | 5.10.2     |

---

## Endpoints

| Método | Caminho                    | Auth          | Descrição                              |
|--------|----------------------------|---------------|----------------------------------------|
| GET    | `/health`                  | Nenhuma       | Status da API                          |
| POST   | `/api/auth/login`          | Nenhuma       | Login                                  |
| POST   | `/api/auth/register`       | Nenhuma       | Registro                               |
| GET    | `/api/users`               | JWT           | Lista todos os usuários                |
| GET    | `/api/users/{id}`          | JWT           | Perfil de usuário                      |
| GET    | `/api/products`            | Nenhuma       | Catálogo de produtos                   |
| GET    | `/api/products/search?q=`  | Nenhuma       | Busca de produtos                      |
| GET    | `/api/products/{id}`       | Nenhuma       | Produto por ID                         |
| GET    | `/api/admin/users`         | Admin         | Lista usuários admin                   |
| GET    | `/api/admin/stats`         | Admin         | Estatísticas                           |
| DELETE | `/api/admin/logs`          | Admin         | Limpar logs                            |
| GET    | `/api/logs`                | Nenhuma       | Logs de requisições                    |
| GET    | `/api/logs/stats`          | Nenhuma       | Estatísticas dos logs                  |
| GET    | `/api/orders`              | JWT           | Pedidos do usuário                     |
| GET    | `/api/orders/{id}`         | JWT           | Pedido por ID                          |
| POST   | `/api/orders`              | JWT           | Criar pedido                           |
| GET    | `/monitor/`                | Nenhuma       | Dashboard de monitoramento             |

---

## Dashboard de Monitoramento

Acesse `http://localhost:4000/monitor/` para o dashboard em tempo real.

O dashboard:
- Faz polling de `/api/logs` a cada 5 s (configurável)
- Realça keywords SQL suspeitas
- Mostra estatísticas agregadas (total de logs, tentativas de login, erros 5xx)

---

## Execução dos Testes

```bash
./gradlew test
```

Os testes verificam aspectos de segurança da aplicação.

---

## Estrutura do Projeto

```
kotlin/
├── build.gradle.kts                        # Dependências e configuração Gradle
├── settings.gradle.kts
├── src/main/kotlin/com/treinamento/ctf/
│   ├── Application.kt                      # Porta 4000, registro de plugins e rotas
│   ├── database/Database.kt                # Init SQLite + seed
│   ├── models/Models.kt                    # User, Product, Order, RequestLog, DTOs
│   ├── auth/JwtManager.kt                  # JWT manager
│   ├── plugins/
│   │   ├── Cors.kt                         # CORS config
│   │   └── RequestLogger.kt               # Plugin de logging de requisições
│   └── routes/
│       ├── AuthRoutes.kt                   # Login + register
│       ├── UserRoutes.kt                   # Endpoints de usuário
│       ├── ProductRoutes.kt                # Busca de produtos
│       ├── AdminRoutes.kt                  # Endpoints admin
│       ├── LogRoutes.kt                    # Logs de requisições
│       └── OrderRoutes.kt                  # CRUD de pedidos
├── src/main/resources/
│   ├── application.conf                    # Porta, JWT secret, DB path
│   └── static/monitor/index.html          # Dashboard de monitoramento
└── src/test/kotlin/com/treinamento/ctf/
    └── SecurityTest.kt                     # Testes de segurança
```
