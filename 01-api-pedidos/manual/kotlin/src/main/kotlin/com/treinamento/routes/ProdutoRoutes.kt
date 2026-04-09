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

fun Route.produtoRoutes() {
    route("/produtos") {

        // GET /api/produtos?busca=X&page=1&limit=10
        get {
            val busca  = call.request.queryParameters["busca"]
            val page   = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val limit  = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtLeast(1) ?: 10

            val result = transaction {
                val query = if (!busca.isNullOrBlank()) {
                    Produtos.select { Produtos.nome like "%${busca}%" }
                } else {
                    Produtos.selectAll()
                }

                val total = query.count().toInt()
                val totalPages = total / limit

                val data = query
                    .orderBy(Produtos.id, SortOrder.ASC)
                    .limit(limit, offset = ((page - 1) * limit).toLong())
                    .map { row -> row.toProduto() }

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

        // GET /api/produtos/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID inválido")

            val produto = transaction {
                Produtos.select { Produtos.id eq id }
                    .singleOrNull()
                    ?.toProduto()
            } ?: throw NoSuchElementException("Produto $id não encontrado")

            call.respond(HttpStatusCode.OK, produto)
        }

        // POST /api/produtos
        post {
            val req = call.receive<ProdutoRequest>()

            val produto = transaction {
                val newId = Produtos.insertAndGetId {
                    it[nome]      = req.nome
                    it[descricao] = req.descricao
                    it[preco]     = req.preco
                    it[estoque]   = req.estoque
                    it[categoria] = req.categoria
                    it[createdAt] = LocalDateTime.now()
                }.value

                Produtos.select { Produtos.id eq newId }
                    .single()
                    .toProduto()
            }
            call.respond(HttpStatusCode.Created, produto)
        }

        // PUT /api/produtos/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID inválido")
            val req = call.receive<ProdutoRequest>()

            val updated = transaction {
                val count = Produtos.update({ Produtos.id eq id }) {
                    it[nome]      = req.nome
                    it[descricao] = req.descricao
                    it[preco]     = req.preco
                    it[estoque]   = req.estoque
                    it[categoria] = req.categoria
                }
                if (count == 0) null
                else Produtos.select { Produtos.id eq id }.single().toProduto()
            } ?: throw NoSuchElementException("Produto $id não encontrado")

            call.respond(HttpStatusCode.OK, updated)
        }

        // DELETE /api/produtos/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("ID inválido")

            val deleted = transaction {
                Produtos.deleteWhere { Produtos.id eq id }
            }
            if (deleted == 0) throw NoSuchElementException("Produto $id não encontrado")

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ResultRow.toProduto() = Produto(
    id        = this[Produtos.id].value,
    nome      = this[Produtos.nome],
    descricao = this[Produtos.descricao],
    preco     = this[Produtos.preco],
    estoque   = this[Produtos.estoque],
    categoria = this[Produtos.categoria],
    createdAt = this[Produtos.createdAt].toString()
)
