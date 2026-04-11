# Exercício 05 — Segurança CTF: Ataque vs Defesa

## Contexto

Este é um exercício **competitivo**. A sala será dividida em duas equipes:

| Equipe | Papel | Objetivo |
|--------|-------|----------|
| **Frontend** (Ataque) | Pentester | Explorar vulnerabilidades da API para acessar dados indevidos |
| **Backend** (Defesa) | DevSecOps | Encontrar e corrigir vulnerabilidades antes que o atacante consiga |

## Como funciona

```
┌─────────────────┐         ┌─────────────────┐
│  EQUIPE ATAQUE  │ ──────> │  EQUIPE DEFESA  │
│  (Frontend)     │ requests│  (Backend API)  │
│                 │ <────── │                 │
│  Explora vulns  │ responses  Corrige vulns  │
│  Extrai dados   │         │  Monitora logs  │
└─────────────────┘         └─────────────────┘
```

- O **time de ataque** usa uma aplicação React com ferramentas integradas
- O **time de defesa** roda a API e tem acesso a um painel de monitoramento
- É uma **corrida contra o tempo**: ataque tenta explorar, defesa tenta corrigir

## Condições de vitória

### Time de Ataque vence se:
- Extrair email + senha do usuário admin
- OU criar um novo usuário com role "admin"
- OU acessar dados de todos os usuários sem autorização

### Time de Defesa vence se:
- Corrigir todas as vulnerabilidades críticas antes do ataque ser efetivado
- O time de ataque não consegue mais nenhum dos objetivos acima

## Setup

### Time de Ataque (Frontend)
```bash
cd frontend/
bun install && bun run dev       # Linux / macOS
bun install; bun run dev         # Windows (PowerShell)
# Acesse http://localhost:5173
# Pressione Ctrl+Shift+D para abrir o DevTools
```

### Time de Defesa (Backend — escolha sua linguagem)
```bash
# Java:
cd backend/java/ && mvn spring-boot:run              # Linux / macOS
cd backend\java\; mvn spring-boot:run                 # Windows (PowerShell)

# Kotlin:
cd backend/kotlin/ && ./gradlew run                   # Linux / macOS
cd backend\kotlin\; .\gradlew run                     # Windows (PowerShell)

# PHP:
cd backend/php/ && composer install && php -S localhost:4000 -t public     # Linux / macOS
cd backend\php\; composer install; php -S localhost:4000 -t public         # Windows (PowerShell)

# Bun:
cd backend/bun/ && bun install && bun run src/index.ts    # Linux / macOS
cd backend\bun\; bun install; bun run src/index.ts        # Windows (PowerShell)

# Monitor disponível em http://localhost:4000/monitor
```

## Regras

1. **Nada de ferramentas externas**: Não usar Burp Suite, sqlmap, etc. Apenas a aplicação fornecida.
2. **Nada de acesso direto ao banco**: O ataque deve ser feito via API.
3. **O backend deve continuar funcional**: Correções não devem quebrar funcionalidades legítimas.
4. **Use AI**: Este exercício incentiva o uso de AI tanto para ataque quanto para defesa.
5. **Sem comunicação entre times**: Não revelar vulnerabilidades ou correções ao outro time.

## Dicas

Cada time receberá dicas progressivas do instrutor durante o exercício.

## Tempo: 20-30 minutos de competição
