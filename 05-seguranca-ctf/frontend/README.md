# Exercicio 05 — Seguranca de Aplicacoes Web

## Contexto

A **SecureShop** e uma aplicacao de e-commerce com frontend React e backend REST API. Como em qualquer sistema real, a aplicacao pode conter vulnerabilidades de seguranca que precisam ser identificadas e corrigidas.

Neste exercicio voce usara ferramentas de AI (Claude Code, Copilot, etc.) para realizar um teste de penetracao (pentest) na aplicacao, identificando e explorando vulnerabilidades presentes no backend.

## Cenario

- Voce tem acesso ao **codigo-fonte do frontend** (esta pasta)
- Voce tem acesso a **URL da API** do backend
- Voce **NAO tem acesso** ao codigo-fonte do backend
- Voce tem credenciais de um usuario regular para login

## Objetivo

Identifique e explore o maximo de vulnerabilidades possivel na API do backend, documentando:
- O endpoint afetado
- O payload ou tecnica utilizada
- A resposta obtida
- O impacto da vulnerabilidade

---

## Como executar

```bash
# 1. Instalar dependencias
bun install

# 2. Iniciar o frontend
bun run dev
```

O frontend estara disponivel em **http://localhost:5173**

> A API do backend deve estar rodando no endereco informado pelo instrutor.

## Credenciais de acesso

Seu login é o mesmo email utilizado para sua conta do claude code e a sua senha é eSalesWorkshopAI-2026

---

## Ferramentas disponiveis

### DevTools do navegador (F12)
- **Network**: veja todas as requisicoes HTTP, headers, body e responses
- **Console**: erros JavaScript e mensagens de debug

### DevTools da aplicacao (Ctrl+Shift+D)
- **Request Log**: historico de todas as chamadas HTTP feitas pela aplicacao, com deteccao automatica de padroes suspeitos
- **Custom Request**: construtor de requisicoes HTTP manuais para testar endpoints com headers e body personalizados

### Codigo-fonte do frontend
- Analise os arquivos em `src/api/client.ts` para mapear todos os endpoints da API
- Analise os componentes React para entender o fluxo de dados

### AI (Claude Code, Copilot, etc.)
- Use a AI para analisar o codigo do frontend e identificar endpoints
- Cole respostas da API na AI e peca para identificar informacoes sensiveis
