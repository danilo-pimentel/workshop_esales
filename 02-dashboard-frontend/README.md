# Ex.02 - Dashboard Frontend (React + Tailwind)

Dashboard administrativo construído com **React 18**, **Tailwind CSS** e **Recharts**, que consome a API REST do Exercício 01.

## Pré-requisitos

- **Bun 1.1+** instalado (`bun --version`)
- **API do Ex.01 rodando** em http://localhost:3000

> ⚠️ **Importante:** Este dashboard consome dados da API do Exercício 01. Antes de iniciar, suba o backend do Ex.01 em qualquer linguagem (Bun, Java, Kotlin ou PHP) na porta 3000.
>
> Exemplo rápido com Bun:
> ```bash
> # Linux / macOS:
> cd ../01-api-pedidos/bun
> bun install
> bun run src/index.ts &
>
> # Windows (PowerShell): rode em um terminal separado
> cd ..\01-api-pedidos\bun
> bun install
> bun run src/index.ts
> ```

## Como executar

```bash
# 1. Instalar dependências
bun install

# 2. Instalar o Playwright (necessário para os testes E2E)
bunx playwright install

# 3. Iniciar o servidor de desenvolvimento
bun run dev
```

O dashboard estará disponível em **http://localhost:5173**

## Como usar

Não há tela de login — o dashboard abre diretamente na página principal. Navegue pelo menu lateral:

| Página | Rota | O que faz |
|--------|------|-----------|
| **Dashboard** | `/` | Métricas resumidas (receita, pedidos, clientes) e gráfico de vendas |
| **Produtos** | `/produtos` | Catálogo com busca, edição inline e exclusão |
| **Pedidos** | `/pedidos` | Lista de pedidos com filtros por status (pendente, pago, enviado, etc.) |
| **Clientes** | `/clientes` | Lista de clientes com busca e histórico de pedidos |

> 💡 Se a página carregar mas não mostrar dados, verifique se a API do Ex.01 está rodando na porta 3000.

## Como testar

```bash
bun run test
# ou com mais detalhes:
bunx playwright test --reporter=list
```

## Desafio

### Parte 1 — Corrigir os bugs

Os testes E2E (`tests/e2e.spec.ts`) expõem 5 bugs intencionais. Execute os testes para identificá-los e corrija o código-fonte.

Testes que **falham** (indicam bugs):
| Teste | Descrição do bug |
|---|---|
| `test_table_sorting` | Clicar no cabeçalho ordena a coluna, mas os dados não mudam de ordem |
| `test_mobile_layout` | Em telas < 768px o conteúdo transborda horizontalmente |
| `test_modal_closes_after_save` | Após salvar um produto, o modal permanece aberto |
| `test_chart_renders` | O gráfico de vendas não exibe barras/linhas com dados |
| `test_loading_states` | Nenhuma página exibe o spinner durante chamadas à API |

Testes que **passam** (smoke tests):
| Teste | Descrição |
|---|---|
| `test_dashboard_loads` | Cards de métricas renderizam |
| `test_product_list` | Tabela de produtos renderiza |
| `test_navigation` | Links do menu navegam corretamente |

### Parte 2 — Restyling

O arquivo `public/mockup.png` contém o design final esperado. Implemente:

1. **Novo estilo do header e sidebar** conforme o mockup
2. **Dark mode funcional** — o botão de lua/sol no header deve alternar o tema. Use a variante `dark:` do Tailwind (o `darkMode: 'class'` já está configurado). Dica: adicione/remova a classe `dark` no elemento `<html>`.
3. **Animações e transições** nos cards (hover effects, fade-in ao carregar)
4. **Layout responsivo mobile-first** — sidebar colapsável em telas pequenas, tabelas com scroll horizontal

### Dicas para as correções

**Bug 1 (sorting):** O estado `sortKey` e `sortDir` são atualizados, mas a variável `data` passada para `.map()` nunca é ordenada. Crie uma cópia ordenada antes do `return`.

**Bug 2 (mobile):** Adicione `sm:block hidden` no `<aside>` e remova o `ml-64` fixo. Use um estado para controlar a visibilidade do menu em mobile.

**Bug 3 (modal):** Após o `await api.put(...)` com sucesso, chame `setIsModalOpen(false)` e `refetch()`.

**Bug 4 (chart):** Os campos do objeto gerado em `buildChartData` são `vendas`, `pedidos` e `ticket`, mas o componente usa `dataKey="total_vendas"` e `dataKey="total_pedidos"`. Alinhe os nomes.

**Bug 5 (loading):** Importe `LoadingSpinner` em cada página e renderize-o condicionalmente quando `isLoading === true`.

## Referência de comandos

| Comando | O que faz |
|---------|-----------|
| `bun install` | Instala dependências |
| `bun run dev` | Inicia dev server (http://localhost:5173) |
| `bun run build` | Build de produção (tsc + vite build) |
| `bun run test` | Executa testes E2E com Playwright |
| `bunx playwright test --reporter=list` | Testes com output detalhado |
| `bunx playwright test --ui` | Testes com interface gráfica |
