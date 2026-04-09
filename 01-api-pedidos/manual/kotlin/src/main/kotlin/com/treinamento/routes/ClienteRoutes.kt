package com.treinamento.routes

import com.treinamento.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.clienteRoutes() {
    route("/clientes") {

        // GET /api/clientes
        get {
            val clientes = transaction {
                Clientes.selectAll()
                    .orderBy(Clientes.nome, SortOrder.ASC)
                    .map { row ->
                        Cliente(
                            id        = row[Clientes.id].value,
                            nome      = row[Clientes.nome],
                            email     = row[Clientes.email],
                            telefone  = row[Clientes.telefone],
                            createdAt = row[Clientes.createdAt].toString()
                        )
                    }
            }
            call.respond(HttpStatusCode.OK, clientes)
        }

        // GET /api/clientes/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID inválido")

            val resultado = transaction {
                val clienteRow = Clientes.select { Clientes.id eq id }
                    .singleOrNull()
                    ?: return@transaction null

                val pedidos = Pedidos.select { Pedidos.clienteId eq id }
                    .orderBy(Pedidos.createdAt, SortOrder.DESC)
                    .map { row ->
                        PedidoSimples(
                            id        = row[Pedidos.id].value,
                            clienteId = row[Pedidos.clienteId],
                            status    = row[Pedidos.status],
                            total     = row[Pedidos.total],
                            createdAt = row[Pedidos.createdAt].toString()
                        )
                    }

                val totalGasto = pedidos.sumOf { it.total }

                ClienteDetalhe(
                    id           = clienteRow[Clientes.id].value,
                    nome         = clienteRow[Clientes.nome],
                    email        = clienteRow[Clientes.email],
                    telefone     = clienteRow[Clientes.telefone],
                    createdAt    = clienteRow[Clientes.createdAt].toString(),
                    pedidos      = pedidos,
                    totalPedidos = pedidos.size,
                    totalGasto   = Math.round(totalGasto * 100.0) / 100.0
                )
            } ?: throw NoSuchElementException("Cliente $id não encontrado")

            call.respond(HttpStatusCode.OK, resultado)
        }
    }
}
