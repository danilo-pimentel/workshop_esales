package com.treinamento.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CRUD de produto: criar, buscar, atualizar e deletar")
    void testCrudProduto() throws Exception {
        // CREATE
        String novoProduto = """
                {
                    "nome": "Produto Teste CRUD",
                    "descricao": "Descricao do produto de teste",
                    "preco": 99.90,
                    "estoque": 10,
                    "categoria": "Testes"
                }
                """;
        MvcResult criarResult = mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoProduto))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode criado = objectMapper.readTree(criarResult.getResponse().getContentAsString());
        long id = criado.get("id").asLong();
        assertEquals("Produto Teste CRUD", criado.get("nome").asText());
        assertEquals(99.90, criado.get("preco").asDouble(), 0.001);

        // READ
        mockMvc.perform(get("/api/produtos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Teste CRUD"));

        // UPDATE
        String atualizacao = """
                {
                    "nome": "Produto Atualizado",
                    "preco": 149.90
                }
                """;
        mockMvc.perform(put("/api/produtos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Atualizado"))
                .andExpect(jsonPath("$.preco").value(149.90));

        // DELETE
        mockMvc.perform(delete("/api/produtos/" + id))
                .andExpect(status().isNoContent());

        // VERIFY DELETED
        mockMvc.perform(get("/api/produtos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Busca case-insensitive: 'notebook' deve encontrar 'Notebook Pro'")
    void testBuscaCaseInsensitive() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/produtos")
                        .param("busca", "notebook")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        JsonNode data = json.get("data");

        boolean encontrou = false;
        for (JsonNode produto : data) {
            if ("Notebook Pro".equals(produto.get("nome").asText())) {
                encontrou = true;
                break;
            }
        }

        assertTrue(encontrou, "Busca por 'notebook' deveria retornar 'Notebook Pro'");
    }

    @Test
    @DisplayName("Criar pedido: deve criar com sucesso e retornar status 201")
    void testCriarPedido() throws Exception {
        String pedidoBody = """
                {
                    "cliente_id": 1,
                    "itens": [
                        {"produto_id": 1, "quantidade": 2},
                        {"produto_id": 2, "quantidade": 1}
                    ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(json.get("id"));
        assertEquals(1, json.get("cliente_id").asInt());
        assertEquals("pendente", json.get("status").asText());
        assertTrue(json.get("total").asDouble() > 0);

        // Verify details via GET
        long pedidoId = json.get("id").asLong();
        MvcResult detalheResult = mockMvc.perform(get("/api/pedidos/" + pedidoId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode detalhe = objectMapper.readTree(detalheResult.getResponse().getContentAsString());
        assertEquals(pedidoId, detalhe.get("id").asLong());
        assertNotNull(detalhe.get("itens"));
        assertNotNull(detalhe.get("cliente_nome"));
    }

    @Test
    @DisplayName("Preco negativo deve ser rejeitado com status 400")
    void testPrecoNegativoRejeitado() throws Exception {
        String body = """
                {
                    "nome": "Produto Preco Negativo",
                    "preco": -10.00,
                    "estoque": 5,
                    "categoria": "Teste"
                }
                """;

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Listar clientes: deve retornar lista com clientes cadastrados")
    void testListarClientes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        assertTrue(json.isArray(), "Resposta deve ser um array");
        assertEquals(20, json.size(), "Deve haver 20 clientes no seed");

        // Detail with order history
        MvcResult detalheResult = mockMvc.perform(get("/api/clientes/1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cliente = objectMapper.readTree(detalheResult.getResponse().getContentAsString());
        assertNotNull(cliente.get("pedidos"), "Cliente deve ter campo pedidos");
        assertNotNull(cliente.get("total_pedidos"), "Cliente deve ter campo total_pedidos");
        assertNotNull(cliente.get("total_gasto"), "Cliente deve ter campo total_gasto");
    }

    @Test
    @DisplayName("Paginacao: totalPages deve usar ceil (ex: 15 items / 10 = 2 paginas)")
    void testPaginacaoTotalPages() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/produtos")
                        .param("page", "1")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        long total = json.get("total").asLong();
        int limit = json.get("limit").asInt();
        int totalPages = json.get("total_pages").asInt();

        int expectedTotalPages = (int) Math.ceil((double) total / limit);
        assertEquals(expectedTotalPages, totalPages,
                "totalPages deveria ser " + expectedTotalPages + " mas foi " + totalPages);
    }

    @Test
    @DisplayName("Dashboard resumo: deve retornar totalVendas, totalPedidos e ticketMedio")
    void testDashboardResumo() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        assertNotNull(json.get("total_vendas"), "Campo total_vendas deve existir");
        assertNotNull(json.get("total_pedidos"), "Campo total_pedidos deve existir");
        assertNotNull(json.get("ticket_medio"), "Campo ticket_medio deve existir");

        assertTrue(json.get("total_vendas").asDouble() > 0, "total_vendas deve ser maior que zero");
        assertTrue(json.get("total_pedidos").asLong() > 0, "total_pedidos deve ser maior que zero");
        assertTrue(json.get("ticket_medio").asDouble() > 0, "ticket_medio deve ser maior que zero");
    }

    @Test
    @DisplayName("Total do pedido deve considerar quantidade: 3 unidades x R$100 = R$300")
    void testTotalPedidoComQuantidade() throws Exception {
        String novoProduto = """
                {
                    "nome": "Produto Para Teste Quantidade",
                    "descricao": "Produto de teste",
                    "preco": 100.00,
                    "estoque": 50,
                    "categoria": "Teste"
                }
                """;
        MvcResult prodResult = mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoProduto))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode prodJson = objectMapper.readTree(prodResult.getResponse().getContentAsString());
        long produtoId = prodJson.get("id").asLong();

        String pedidoBody = String.format("""
                {
                    "cliente_id": 1,
                    "itens": [
                        {"produto_id": %d, "quantidade": 3}
                    ]
                }
                """, produtoId);

        MvcResult pedidoResult = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode pedidoJson = objectMapper.readTree(pedidoResult.getResponse().getContentAsString());
        double total = pedidoJson.get("total").asDouble();

        assertEquals(300.00, total, 0.001,
                "Total deveria ser 300.00 (3 x 100.00), mas foi " + total);
    }

    @Test
    @DisplayName("Filtro por status: GET /api/pedidos?status=entregue deve retornar apenas pedidos entregues")
    void testFiltroStatus() throws Exception {
        MvcResult allResult = mockMvc.perform(get("/api/pedidos")
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode allJson = objectMapper.readTree(allResult.getResponse().getContentAsString());
        long totalGeral = allJson.get("total").asLong();

        MvcResult result = mockMvc.perform(get("/api/pedidos")
                        .param("status", "entregue")
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        JsonNode data = json.get("data");

        for (JsonNode pedido : data) {
            assertEquals("entregue", pedido.get("status").asText(),
                    "Filtro por status=entregue retornou pedido com status diferente");
        }

        assertTrue(json.get("total").asLong() < totalGeral,
                "Filtro por status deveria retornar menos pedidos que o total");
    }
}
