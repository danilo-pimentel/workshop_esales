package com.treinamento.routes

import com.treinamento.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Route.pedidoRoutes() {
    route("/pedidos") {

        // GET /api/pedidos?page=1&limit=10
        get {
            val page   = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val limit  = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtLeast(1) ?: 10

            @Suppress("UNUSED_VARIABLE")
            val status     = call.request.queryParameters["status"]
            @Suppress("UNUSED_VARIABLE")
            val dataInicio = call.request.queryParameters["data_inicio"]
            @Suppress("UNUSED_VARIABLE")
            val dataFim    = call.request.queryParameters["data_fim"]

            val result = transaction {
                val total = Pedidos.selectAll().count().toInt()
                val totalPages = total / limit

                val data = (Pedidos innerJoin Clientes)
                    .selectAll()
                    .orderBy(Pedidos.id, SortOrder.DESC)
                    .limit(limit, offset = ((page - 1) * limit).toLong())
                    .map { row ->
                        PedidoListItem(
                            id         = row[Pedidos.id].value,
                            clienteId  = row[Pedidos.clienteId],
                            status     = row[Pedidos.status],
                            total      = row[Pedidos.total],
                            createdAt  = row[Pedidos.createdAt].toString(),
                            clienteNome = row[Clientes.nome]
                        )
                    }

                PaginatedResponse(
                    data       = data,
                    page       = page,
                    limit      = limit,
                    total      = total,
                    totalPages = totalPages
                )
            }
            call.respond(HttpStatusCode.OK, result)
        }

        // GET /api/pedidos/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID inválido")

            val detalhe = transaction {
                val row = (Pedidos innerJoin Clientes)
                    .select { Pedidos.id eq id }
                    .singleOrNull()
                    ?: return@transaction null

                val itens = (ItensPedido innerJoin Produtos)
                    .select { ItensPedido.pedidoId eq id }
                    .map { itemRow ->
                        ItemPedidoDetalhe(
                            id            = itemRow[ItensPedido.id].value,
                            pedidoId      = itemRow[ItensPedido.pedidoId],
                            produtoId     = itemRow[ItensPedido.produtoId],
                            quantidade    = itemRow[ItensPedido.quantidade],
                            precoUnitario = itemRow[ItensPedido.precoUnitario],
                            produtoNome   = itemRow[Produtos.nome]
                        )
                    }

                PedidoDetalhe(
                    id              = row[Pedidos.id].value,
                    clienteId       = row[Pedidos.clienteId],
                    status          = row[Pedidos.status],
                    total           = row[Pedidos.total],
                    createdAt       = row[Pedidos.createdAt].toString(),
                    clienteNome     = row[Clientes.nome],
                    clienteEmail    = row[Clientes.email],
                    clienteTelefone = row[Clientes.telefone],
                    itens           = itens
                )
            } ?: throw NoSuchElementException("Pedido $id não encontrado")

            call.respond(HttpStatusCode.OK, detalhe)
        }

        // POST /api/pedidos
        post {
            val req = call.receive<CriarPedidoRequest>()

            if (req.itens.isEmpty()) {
                throw IllegalArgumentException("Pedido deve ter pelo menos um item")
            }

            val pedidoCriado = transaction {
                // Validate client exists
                Clientes.select { Clientes.id eq req.clienteId }
                    .singleOrNull()
                    ?: throw IllegalArgumentException("Cliente não encontrado")

                // Fetch product prices and validate stock
                data class ItemInfo(val produtoId: Int, val quantidade: Int, val precoUnitario: Double, val estoque: Int, val nome: String)

                val itensList = req.itens.map { item ->
                    val prodRow = Produtos.select { Produtos.id eq item.produtoId }
                        .singleOrNull()
                        ?: throw IllegalArgumentException("Um ou mais produtos não encontrados")
                    ItemInfo(
                        produtoId = item.produtoId,
                        quantidade = item.quantidade,
                        precoUnitario = prodRow[Produtos.preco],
                        estoque = prodRow[Produtos.estoque],
                        nome = prodRow[Produtos.nome]
                    )
                }

                // Stock validation
                for (item in itensList) {
                    if (item.estoque < item.quantidade) {
                        throw IllegalArgumentException("Estoque insuficiente para o produto ${item.nome}")
                    }
                }

                val total = itensList.sumOf { it.precoUnitario }

                val pedidoId = Pedidos.insertAndGetId {
                    it[clienteId] = req.clienteId
                    it[status]    = "pendente"
                    it[Pedidos.total]     = total
                    it[createdAt] = LocalDateTime.now()
                }.value

                // Insert items and decrement stock
                itensList.forEach { item ->
                    ItensPedido.insert {
                        it[ItensPedido.pedidoId]      = pedidoId
                        it[ItensPedido.produtoId]     = item.produtoId
                        it[ItensPedido.quantidade]    = item.quantidade
                        it[ItensPedido.precoUnitario] = item.precoUnitario
                    }

                    Produtos.update({ Produtos.id eq item.produtoId }) {
                        it[Produtos.estoque] = item.estoque - item.quantidade
                    }
                }

                // Return bare pedido only (matches Bun reference)
                val pedidoRow = Pedidos.select { Pedidos.id eq pedidoId }.single()
                Pedido(
                    id        = pedidoRow[Pedidos.id].value,
                    clienteId = pedidoRow[Pedidos.clienteId],
                    status    = pedidoRow[Pedidos.status],
                    total     = pedidoRow[Pedidos.total],
                    createdAt = pedidoRow[Pedidos.createdAt].toString()
                )
            }

            call.respond(HttpStatusCode.Created, pedidoCriado)
        }

        // PATCH /api/pedidos/{id}/status
        patch("{id}/status") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID inválido")
            val req = call.receive<AtualizarStatusRequest>()

            val statusValidos = listOf("pendente", "processando", "enviado", "entregue", "cancelado")
            if (req.status !in statusValidos) {
                throw IllegalArgumentException("Status inválido. Valores permitidos: ${statusValidos.joinToString(", ")}")
            }

            val updated = transaction {
                val count = Pedidos.update({ Pedidos.id eq id }) {
                    it[status] = req.status
                }
                if (count == 0) null
                else Pedidos.select { Pedidos.id eq id }.single().let { row ->
                    Pedido(
                        id        = row[Pedidos.id].value,
                        clienteId = row[Pedidos.clienteId],
                        status    = row[Pedidos.status],
                        total     = row[Pedidos.total],
                        createdAt = row[Pedidos.createdAt].toString()
                    )
                }
            } ?: throw NoSuchElementException("Pedido $id não encontrado")

            call.respond(HttpStatusCode.OK, updated)
        }
    }
}
