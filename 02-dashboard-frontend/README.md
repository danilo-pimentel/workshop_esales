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

Existem **5 bugs intencionais** na aplicação. Execute os testes E2E para identificá-los:

```bash
bun run test
# ou com mais detalhes:
bunx playwright test --reporter=list
```

### Bugs e como reproduzir

#### Bug 1 — Ordenação da tabela de produtos

**Teste:** `test_table_sorting`

**Como reproduzir manualmente:**
1. Acesse **http://localhost:5173/produtos**
2. Clique no cabeçalho da coluna "Nome" (ou "Preço", "Estoque")
3. Observe que o indicador visual de ordenação muda (seta aparece), mas a **ordem das linhas da tabela não muda**
4. Clique novamente e veja que continua sem reordenar

**Resultado esperado:** Ao clicar no cabeçalho, as linhas devem ser reordenadas ascendente/descendente pela coluna clicada.

---

#### Bug 2 — Layout mobile (< 768px)

**Teste:** `test_mobile_layout`

**Como reproduzir manualmente:**
1. Abra o dashboard em **http://localhost:5173**
2. Abra o DevTools do navegador (F12) e ative o modo responsivo
3. Defina a largura para menos de 768px (ex: iPhone SE, 375px)
4. Observe que o conteúdo **transborda horizontalmente** e aparece scroll horizontal indesejado
5. Tente navegar no menu lateral — ele ocupa espaço desnecessário em mobile

**Resultado esperado:** Em telas < 768px, o sidebar deve colapsar/esconder e o conteúdo deve se ajustar à largura da tela sem scroll horizontal.

---

#### Bug 3 — Modal não fecha após salvar produto

**Teste:** `test_modal_closes_after_save`

**Como reproduzir manualmente:**
1. Acesse **http://localhost:5173/produtos**
2. Clique no botão "Editar" de qualquer produto
3. Altere algum campo (ex: nome ou preço)
4. Clique em "Salvar"
5. Observe que a requisição é enviada com sucesso, mas o **modal permanece aberto** na tela

**Resultado esperado:** Após salvar com sucesso, o modal deve fechar automaticamente e a lista de produtos deve ser atualizada com os novos dados.

---

#### Bug 4 — Gráfico de vendas não renderiza

**Teste:** `test_chart_renders`

**Como reproduzir manualmente:**
1. Acesse a página inicial **http://localhost:5173/**
2. Role até a seção do gráfico de vendas
3. Observe que o gráfico aparece vazio (sem barras, sem linhas, sem dados visíveis)
4. Os eixos estão desenhados, mas não há dados renderizados

**Resultado esperado:** O gráfico deve exibir as vendas, pedidos e ticket médio com base nos dados retornados pela API.

---

#### Bug 5 — Nenhum loading state

**Teste:** `test_loading_states`

**Como reproduzir manualmente:**
1. Abra **http://localhost:5173** (ou qualquer página do dashboard)
2. No DevTools, em Network, defina a velocidade para "Slow 3G"
3. Recarregue a página (F5)
4. Observe que durante o carregamento **não há nenhum indicador visual** (spinner, skeleton, mensagem de "carregando...")
5. A tela fica em branco até os dados chegarem

**Resultado esperado:** Enquanto os dados estão sendo carregados da API, deve aparecer um spinner ou skeleton em cada página.

---

### Testes que **passam** (smoke tests — nao mexer)

| Teste | Descrição |
|---|---|
| `test_dashboard_loads` | Cards de métricas renderizam |
| `test_product_list` | Tabela de produtos renderiza |
| `test_navigation` | Links do menu navegam corretamente |

---

### Parte 2 — Restyling

O arquivo `public/mockup.png` contém o design final esperado. Implemente:

1. **Novo estilo do header e sidebar** conforme o mockup
2. **Dark mode funcional** — o botão de lua/sol no header deve alternar o tema (o `darkMode: 'class'` do Tailwind já está configurado)
3. **Animações e transições** nos cards (hover effects, fade-in ao carregar)
4. **Layout responsivo mobile-first** — sidebar colapsável em telas pequenas, tabelas com scroll horizontal

## Referência de comandos

| Comando | O que faz |
|---------|-----------|
| `bun install` | Instala dependências |
| `bun run dev` | Inicia dev server (http://localhost:5173) |
| `bun run build` | Build de produção (tsc + vite build) |
| `bun run test` | Executa testes E2E com Playwright |
| `bunx playwright test --reporter=list` | Testes com output detalhado |
| `bunx playwright test --ui` | Testes com interface gráfica |
