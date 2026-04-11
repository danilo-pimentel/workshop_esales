# CTF Vulnerable Backend — Java / Spring Boot

> **AVISO**: Esta aplicacao e DELIBERADAMENTE VULNERAVEL.
> Criada exclusivamente para fins de treinamento de segurança em ambiente controlado.
> **NAO IMPLANTE EM PRODUCAO.**

---

## Visao Geral

API REST vulneravel construida com Java 21 + Spring Boot 3.2 para exercicios de CTF
(Capture The Flag) em treinamentos de segurança ofensiva e defensiva.

- **Porta**: `4000`
- **Banco de dados**: H2 in-memory (sem persistencia entre reinicializacoes)
- **Dashboard de monitoramento**: `http://localhost:4000/monitor/index.html`
- **Console H2**: `http://localhost:4000/h2-console` (usuario: `sa`, senha: em branco)

---

## Inicializacao Rapida

```bash
# Instalar dependencias e iniciar
mvn spring-boot:run

# Executar testes (todos devem PASSAR — confirma que as vulnerabilidades existem)
mvn test
```

---

## Credenciais de Seed

| Funcao | Email                | Senha       |
|--------|----------------------|-------------|
| Admin  | admin@empresa.com    | Admin@2024! |
| User   | alice@email.com      | alice123    |
| User   | bob@email.com        | bob@secure99|
| User   | carol@email.com      | carol2024   |
| User   | david@email.com      | david!pass  |
| User   | eva@email.com        | eva#secret  |

---

## Endpoints da API

### Autenticacao (publico)
| Metodo | Endpoint            | Descricao                            |
|--------|---------------------|--------------------------------------|
| POST   | /api/auth/login     | Login                                |
| POST   | /api/auth/register  | Registro de novo usuario             |

### Usuarios (requer JWT)
| Metodo | Endpoint         | Descricao                                  |
|--------|------------------|--------------------------------------------|
| GET    | /api/users/me    | Dados do usuario autenticado               |
| GET    | /api/users       | Listar todos os usuarios                   |
| GET    | /api/users/{id}  | Buscar por ID                              |
| PUT    | /api/users/{id}  | Atualizar usuario                          |

### Produtos (publico para leitura)
| Metodo | Endpoint            | Descricao                                   |
|--------|---------------------|---------------------------------------------|
| GET    | /api/products       | Listar/buscar produtos                      |
| GET    | /api/products/{id}  | Buscar por ID                               |
| POST   | /api/products       | Criar produto                               |

### Pedidos (requer JWT)
| Metodo | Endpoint         | Descricao                      |
|--------|------------------|--------------------------------|
| GET    | /api/orders      | Pedidos do usuario atual       |
| GET    | /api/orders/{id} | Pedido por ID                  |
| POST   | /api/orders      | Criar pedido                   |

### Admin (requer permissao admin)
| Metodo | Endpoint              | Descricao                                      |
|--------|-----------------------|------------------------------------------------|
| GET    | /api/admin/users      | Listar usuarios                                |
| GET    | /api/admin/stats      | Estatisticas                                   |
| DELETE | /api/admin/users/{id} | Remover usuario                                |
| POST   | /api/admin/reset-db   | Limpar logs                                    |

### Logs
| Metodo | Endpoint          | Descricao                                   |
|--------|-------------------|---------------------------------------------|
| GET    | /api/logs         | Logs de requisicoes                         |
| GET    | /api/logs/{id}    | Log individual                              |
| GET    | /api/logs/search  | Busca nos logs                              |

---

## Dashboard de Monitoramento

Acesse: `http://localhost:4000/monitor/index.html`

Funcionalidades:
- Tabela em tempo real dos logs de requisicao (polling a cada 3s)
- Destaque de palavras-chave SQL em vermelho
- Tags automaticas: SQL INJECTION, IDOR, ADMIN
- Painel de detalhe com body completo, SQL executado, preview de resposta
- Contadores: total, sqli detectados, erros 4xx/5xx, logins, acessos admin

---

## Estrutura do Projeto

```
src/main/java/com/treinamento/ctf/
├── Application.java                     # Entry point
├── config/CorsConfig.java               # CORS permissivo
├── model/
│   ├── User.java
│   ├── Product.java
│   └── RequestLog.java                  # Log de requisicoes
├── controller/
│   ├── AuthController.java              # Login + register
│   ├── UserController.java              # Endpoints de usuario
│   ├── ProductController.java           # Busca de produtos
│   ├── OrderController.java             # CRUD de pedidos
│   ├── AdminController.java             # Endpoints admin
│   └── LogController.java              # Logs de requisicoes
└── middleware/
    ├── JwtUtil.java                     # JWT manager
    ├── AuthFilter.java                  # Filtro de autenticacao
    └── RequestLogFilter.java            # Logger de requisicoes
```

---

## Execucao dos Testes

```bash
mvn test
```

Os testes em `SecurityTest.java` verificam aspectos de seguranca da aplicacao.
