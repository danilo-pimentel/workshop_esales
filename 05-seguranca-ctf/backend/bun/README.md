# Ex.05 - Segurança CTF: Backend (Bun)

## Contexto

Sua API está sendo atacada! O time de frontend está tentando explorar
vulnerabilidades para acessar dados sensíveis.

Este exercício simula um ambiente de produção com **7 vulnerabilidades intencionais**.
O time defensor precisa encontrar e corrigir os problemas antes que o atacante consiga:

- Acessar dados de outros usuários
- Extrair credenciais do banco de dados
- Criar um usuário administrador sem ter permissão

---

## Como executar

```bash
bun install
bun run src/index.ts
```

- **API**: http://localhost:4000
- **Monitor**: http://localhost:4000/monitor

---

## Monitor

Acesse **http://localhost:4000/monitor** para ver os requests em tempo real.

O painel exibe:
- Tabela de logs atualizada a cada 2 segundos
- Destaque vermelho em requests com palavras-chave SQL suspeitas
- Contagem de requests por endpoint
- Alertas visuais quando padrões de ataque são detectados

Fique de olho em padrões suspeitos!

---

## Como testar (após correções)

```bash
bun test
```

Os testes verificam que as vulnerabilidades **existem**. Após as correções,
os testes devem **falhar** — o que confirma que o bug foi corrigido.

---

## Dicas (reveladas progressivamente pelo instrutor)

- **Minuto 0**: Monitore os logs em `/monitor`. Algo estranho está acontecendo com as queries.
- **Minuto 5**: Verifique como as queries SQL são construídas nas rotas de login e busca.
- **Minuto 10**: Pesquise sobre *prepared statements* e *IDOR*. Veja a rota `/api/users/:id`.
- **Minuto 15**: O atacante já pode estar vendo dados que não deveria... Verifique `/api/logs`.
- **Minuto 20**: Veja como o endpoint `/api/admin/users` verifica permissões. Tem algo errado.
- **Minuto 25**: As senhas precisam ser hasheadas. Pesquise sobre `bcrypt` ou `argon2`.

---

## Endpoints disponíveis

```
POST   /api/auth/login               # público
POST   /api/auth/register            # público
GET    /api/users/me                 # requer JWT
GET    /api/users/:id                # requer JWT
PUT    /api/users/:id                # requer JWT
GET    /api/products                 # público
GET    /api/products/search?q=       # público
GET    /api/products/:id             # público
GET    /api/orders                   # requer JWT
GET    /api/orders/:id               # requer JWT
POST   /api/orders                   # requer JWT
GET    /api/admin/users              # requer permissão admin
POST   /api/admin/users              # requer permissão admin
DELETE /api/admin/users/:id          # requer permissão admin
GET    /api/logs                     # logs de requisições
DELETE /api/logs                     # limpar logs
GET    /monitor                      # Dashboard HTML
```

---

## Usuários de seed

| Email | Senha | Role |
|-------|-------|------|
| admin@empresa.com | Admin@2024! | admin |
| carlos@empresa.com | Senha123 | user |
| ana@empresa.com | Minhasenha1 | user |
| pedro@empresa.com | Pedro@456 | user |
| mariana@empresa.com | MarCosta99 | user |
| rafael@empresa.com | Rafael2024 | user |

---

## Estrutura do projeto

```
bun/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts              # Entrypoint, porta 4000
│   ├── db.ts                 # SQLite + seed
│   ├── routes/
│   │   ├── auth.ts           # Login + register
│   │   ├── users.ts          # Endpoints de usuário
│   │   ├── products.ts       # Busca de produtos
│   │   ├── orders.ts         # Pedidos
│   │   ├── admin.ts          # Admin
│   │   └── logs.ts           # Logs
│   ├── middleware/
│   │   ├── auth.ts           # JWT helper
│   │   └── logger.ts         # Logger de requisições
│   └── monitor/
│       └── dashboard.html    # Painel de monitoramento
└── test/
    └── security.test.ts      # Testes de segurança
```
