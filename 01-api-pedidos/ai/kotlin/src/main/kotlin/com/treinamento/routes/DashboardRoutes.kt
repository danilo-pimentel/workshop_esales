package com.treinamento.routes

import com.treinamento.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.dashboardRoutes() {
    route("/dashboard") {

        // GET /api/dashboard/resumo
        get("/resumo") {
            val resumo = transaction {
                // Exclude cancelado from totals (matches Bun reference)
                val pedidosAtivos = Pedidos.select { Pedidos.status neq "cancelado" }.toList()
                val totalPedidos = pedidosAtivos.size
                val totalVendas = pedidosAtivos.sumOf { it[Pedidos.total] }
                val ticketMedio = if (totalPedidos > 0) {
                    Math.round(totalVendas / totalPedidos * 100.0) / 100.0
                } else 0.0

                // por_status: ALL statuses, ORDER BY quantidade DESC
                val porStatus = Pedidos.slice(Pedidos.status, Pedidos.id.count())
                    .selectAll()
                    .groupBy(Pedidos.status)
                    .orderBy(Pedidos.id.count(), SortOrder.DESC)
                    .map { row ->
                        StatusCount(
                            status = row[Pedidos.status],
                            quantidade = row[Pedidos.id.count()].toInt()
                        )
                    }

                // top_produtos: top 10 by total_vendido, exclude cancelado
                // Fetch all items from non-cancelado orders with product info
                val activePedidoIds = pedidosAtivos.map { it[Pedidos.id].value }.toSet()

                val allItems = (ItensPedido innerJoin Produtos)
                    .selectAll()
                    .filter { row -> row[ItensPedido.pedidoId] in activePedidoIds }
                    .map { row ->
                        Triple(
                            row[ItensPedido.produtoId],
                            row[Produtos.nome],
                            row[ItensPedido.quantidade] to row[ItensPedido.precoUnitario]
                        )
                    }

                // Aggregate by product
                data class ProdutoAgg(var totalVendido: Int = 0, var receita: Double = 0.0, var nome: String = "")

                val produtoMap = mutableMapOf<Int, ProdutoAgg>()
                for ((produtoId, produtoNome, qtyPrice) in allItems) {
                    val (qty, price) = qtyPrice
                    val agg = produtoMap.getOrPut(produtoId) { ProdutoAgg(nome = produtoNome) }
                    agg.totalVendido += qty
                    agg.receita += qty * price
                }

                val topProdutos = produtoMap.entries
                    .sortedByDescending { it.value.totalVendido }
                    .take(10)
                    .map { (produtoId, agg) ->
                        TopProduto(
                            produtoId = produtoId,
                            produtoNome = agg.nome,
                            totalVendido = agg.totalVendido,
                            receita = Math.round(agg.receita * 100.0) / 100.0
                        )
                    }

                DashboardResumo(
                    totalVendas = Math.round(totalVendas * 100.0) / 100.0,
                    totalPedidos = totalPedidos,
                    ticketMedio = ticketMedio,
                    porStatus = porStatus,
                    topProdutos = topProdutos
                )
            }
            call.respond(HttpStatusCode.OK, resumo)
        }
    }
}
