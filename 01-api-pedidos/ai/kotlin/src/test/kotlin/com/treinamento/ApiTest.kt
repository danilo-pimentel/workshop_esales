package com.treinamento

import com.treinamento.models.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

class ApiTest {

    private fun buildClient(builder: ApplicationTestBuilder) =
        builder.createClient {
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    namingStrategy = JsonNamingStrategy.SnakeCase
                })
            }
        }

    @Test
    fun testCrudProduto() = testApplication {
        application { module() }
        val client = buildClient(this)

        // CREATE
        val createResp = client.post("/api/produtos") {
            contentType(ContentType.Application.Json)
            setBody(
                ProdutoRequest(
                    nome      = "Produto Teste CRUD",
                    descricao = "Descrição do produto de teste",
                    preco     = 99.90,
                    estoque   = 10,
                    categoria = "Testes"
                )
            )
        }
        assertEquals(HttpStatusCode.Created, createResp.status)
        val created = createResp.body<Produto>()
        assertEquals("Produto Teste CRUD", created.nome)
        assertEquals(99.90, created.preco)
        val prodId = created.id

        // READ
        val getResp = client.get("/api/produtos/$prodId")
        assertEquals(HttpStatusCode.OK, getResp.status)
        val fetched = getResp.body<Produto>()
        assertEquals(prodId, fetched.id)
        assertEquals("Produto Teste CRUD", fetched.nome)

        // UPDATE
        val updateResp = client.put("/api/produtos/$prodId") {
            contentType(ContentType.Application.Json)
            setBody(
                ProdutoRequest(
                    nome      = "Produto Atualizado",
                    descricao = "Descrição atualizada",
                    preco     = 149.90,
                    estoque   = 20,
                    categoria = "Testes"
                )
            )
        }
        assertEquals(HttpStatusCode.OK, updateResp.status)
        val updated = updateResp.body<Produto>()
        assertEquals("Produto Atualizado", updated.nome)
        assertEquals(149.90, updated.preco)

        // DELETE
        val deleteResp = client.delete("/api/produtos/$prodId")
        assertEquals(HttpStatusCode.NoContent, deleteResp.status)

        // Confirm gone
        val goneResp = client.get("/api/produtos/$prodId")
        assertEquals(HttpStatusCode.NotFound, goneResp.status)
    }

    @Test
    fun testBuscaCaseInsensitive() = testApplication {
        application { module() }
        val client = buildClient(this)

        val resp = client.get("/api/produtos?busca=notebook")
        assertEquals(HttpStatusCode.OK, resp.status)
        val page = resp.body<PaginatedResponse<Produto>>()

        assertTrue(
            page.total > 0,
            "Busca por 'notebook' deveria encontrar 'Notebook Pro'"
        )
    }

    @Test
    fun testCriarPedido() = testApplication {
        application { module() }
        val client = buildClient(this)

        val pedidoResp = client.post("/api/pedidos") {
            contentType(ContentType.Application.Json)
            setBody(
                CriarPedidoRequest(
                    clienteId = 1,
                    itens     = listOf(
                        ItemRequest(produtoId = 1, quantidade = 2),
                        ItemRequest(produtoId = 2, quantidade = 1)
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.Created, pedidoResp.status)
        val pedido = pedidoResp.body<Pedido>()
        assertEquals(1, pedido.clienteId)
        assertEquals("pendente", pedido.status)
        assertTrue(pedido.total > 0, "Total deve ser maior que zero")

        // Verify details via GET
        val detalheResp = client.get("/api/pedidos/${pedido.id}")
        assertEquals(HttpStatusCode.OK, detalheResp.status)
        val detalhe = detalheResp.body<PedidoDetalhe>()
        assertEquals(pedido.id, detalhe.id)
        assertTrue(detalhe.itens.isNotEmpty())
        assertTrue(detalhe.clienteNome.isNotBlank())
    }

    @Test
    fun testPrecoNegativoRejeitado() = testApplication {
        application { module() }
        val client = buildClient(this)

        val resp = client.post("/api/produtos") {
            contentType(ContentType.Application.Json)
            setBody(
                ProdutoRequest(
                    nome      = "Produto Preco Negativo",
                    descricao = "Produto com preco invalido",
                    preco     = -10.00,
                    estoque   = 5,
                    categoria = "Teste"
                )
            )
        }
        assertEquals(
            HttpStatusCode.BadRequest,
            resp.status,
            "API deve rejeitar produto com preco negativo"
        )
    }

    @Test
    fun testListarClientes() = testApplication {
        application { module() }
        val client = buildClient(this)

        val resp = client.get("/api/clientes")
        assertEquals(HttpStatusCode.OK, resp.status)
        val clientes = resp.body<List<Cliente>>()
        assertEquals(20, clientes.size)

        // Detail with order history
        val detalheResp = client.get("/api/clientes/1")
        assertEquals(HttpStatusCode.OK, detalheResp.status)
        val detalhe = detalheResp.body<ClienteDetalhe>()
        assertTrue(detalhe.pedidos.isNotEmpty(), "Cliente deve ter pedidos")
        assertTrue(detalhe.totalPedidos > 0, "Cliente deve ter total_pedidos > 0")
        assertTrue(detalhe.totalGasto > 0, "Cliente deve ter total_gasto > 0")
    }

    @Test
    fun testPaginacaoTotalPages() = testApplication {
        application { module() }
        val client = buildClient(this)

        val resp = client.get("/api/produtos?page=1&limit=7")
        assertEquals(HttpStatusCode.OK, resp.status)
        val page = resp.body<PaginatedResponse<Produto>>()

        val expectedTotalPages = kotlin.math.ceil(page.total.toDouble() / page.limit).toInt()

        assertEquals(
            expectedTotalPages,
            page.totalPages,
            "totalPages deveria ser ceil(${page.total}/${page.limit})=$expectedTotalPages, mas foi ${page.totalPages}"
        )
    }

    @Test
    fun testDashboardResumo() = testApplication {
        application { module() }
        val client = buildClient(this)

        val resp = client.get("/api/dashboard/resumo")
        assertEquals(HttpStatusCode.OK, resp.status)
        val resumo = resp.body<DashboardResumo>()
        assertTrue(resumo.totalVendas > 0, "total_vendas deve ser positivo")
        assertTrue(resumo.totalPedidos > 0, "total_pedidos deve ser positivo")
        assertTrue(resumo.ticketMedio > 0, "ticket_medio deve ser positivo")
    }

    @Test
    fun testTotalPedidoComQuantidade() = testApplication {
        application { module() }
        val client = buildClient(this)

        val createResp = client.post("/api/produtos") {
            contentType(ContentType.Application.Json)
            setBody(
                ProdutoRequest(
                    nome      = "Produto Para Teste Quantidade",
                    descricao = "Produto de teste",
                    preco     = 100.00,
                    estoque   = 50,
                    categoria = "Teste"
                )
            )
        }
        assertEquals(HttpStatusCode.Created, createResp.status)
        val produto = createResp.body<Produto>()

        val quantidade = 3
        val pedidoResp = client.post("/api/pedidos") {
            contentType(ContentType.Application.Json)
            setBody(
                CriarPedidoRequest(
                    clienteId = 1,
                    itens     = listOf(ItemRequest(produtoId = produto.id, quantidade = quantidade))
                )
            )
        }
        assertEquals(HttpStatusCode.Created, pedidoResp.status)
        val pedido = pedidoResp.body<Pedido>()

        assertEquals(
            300.00,
            pedido.total,
            0.01,
            "Total do pedido deveria ser 300.00 (3 x 100.00), mas foi ${pedido.total}"
        )
    }

    @Test
    fun testFiltroStatus() = testApplication {
        application { module() }
        val client = buildClient(this)

        val allResp = client.get("/api/pedidos?limit=200")
        assertEquals(HttpStatusCode.OK, allResp.status)
        val allPedidos = allResp.body<PaginatedResponse<Pedido>>()
        val totalGeral = allPedidos.total

        val filteredResp = client.get("/api/pedidos?status=entregue&limit=200")
        assertEquals(HttpStatusCode.OK, filteredResp.status)
        val filteredPedidos = filteredResp.body<PaginatedResponse<Pedido>>()

        assertTrue(
            filteredPedidos.total < totalGeral,
            "Filtro por status deve retornar menos pedidos que o total"
        )
        assertTrue(
            filteredPedidos.data.all { it.status == "entregue" },
            "Todos os pedidos retornados devem ter status 'entregue'"
        )
    }
}
