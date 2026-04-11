package com.treinamento.ctf.routes

import com.treinamento.ctf.auth.*
import com.treinamento.ctf.database.dbConnection
import com.treinamento.ctf.models.*
import com.treinamento.ctf.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun sanitizeInput(input: String): String {
    return input.replace(Regex("[<>\"&]"), "")
}

fun Route.productRoutes() {

    route("/api/products") {

        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val offset = (page - 1) * limit

            val sqlQuery = "SELECT * FROM products LIMIT $limit OFFSET $offset"
            call.logSql(sqlQuery)

            val products = mutableListOf<Map<String, Any?>>()
            runCatching {
                val rs = dbConnection.prepareStatement(
                    "SELECT * FROM products LIMIT ? OFFSET ?"
                ).apply { setInt(1, limit); setInt(2, offset) }.executeQuery()
                while (rs.next()) {
                    products.add(mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "descricao" to rs.getString("descricao"),
                        "preco" to rs.getDouble("preco"),
                        "categoria" to rs.getString("categoria"),
                        "created_at" to rs.getString("created_at")
                    ))
                }
            }.onFailure { e ->
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            val total = runCatching {
                dbConnection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM products")
                    .also { it.next() }.getInt(1)
            }.getOrDefault(0)

            call.logStatus(200)
            call.logResponse("${products.size} products")
            call.respond(HttpStatusCode.OK, mapOf(
                "products" to products, "total" to total, "page" to page, "limit" to limit
            ))
        }

        get("/search") {
            val rawSearch = call.request.queryParameters["q"] ?: ""
            val search = sanitizeInput(rawSearch)

            val sqlQuery = "SELECT id, nome, descricao, preco, categoria, created_at FROM products WHERE nome LIKE '%$search%'"
            call.logSql(sqlQuery)

            val results = mutableListOf<Map<String, Any?>>()
            try {
                val rs = dbConnection.createStatement().executeQuery(sqlQuery)
                while (rs.next()) {
                    results.add(mapOf(
                        "id" to rs.getInt(1),
                        "nome" to (runCatching { rs.getString(2) }.getOrDefault("")),
                        "descricao" to (runCatching { rs.getString(3) }.getOrNull()),
                        "preco" to (runCatching { rs.getDouble(4) }.getOrDefault(0.0)),
                        "categoria" to (runCatching { rs.getString(5) }.getOrNull()),
                        "created_at" to (runCatching { rs.getString(6) }.getOrNull())
                    ))
                }
            } catch (e: Exception) {
                call.logStatus(500)
                call.logResponse(e.message ?: "")
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            call.logStatus(200)
            call.logResponse("${results.size} results")
            call.respond(HttpStatusCode.OK, mapOf(
                "results" to results, "count" to results.size, "query" to search
            ))
        }

        get("/{id}") {
            val idStr = call.parameters["id"] ?: ""
            if (idStr == "search" || idStr == "import") return@get

            val id = idStr.toIntOrNull()
            if (id == null) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Invalid product id"))
                return@get
            }

            val sqlQuery = "SELECT * FROM products WHERE id = $id"
            call.logSql(sqlQuery)

            val product: Map<String, Any?>?
            try {
                val rs = dbConnection.prepareStatement(
                    "SELECT * FROM products WHERE id = ?"
                ).apply { setInt(1, id) }.executeQuery()
                product = if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "descricao" to rs.getString("descricao"),
                        "preco" to rs.getDouble("preco"),
                        "categoria" to rs.getString("categoria"),
                        "created_at" to rs.getString("created_at")
                    )
                } else null
            } catch (e: Exception) {
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            if (product == null) {
                call.logStatus(404)
                call.respond(HttpStatusCode.NotFound,
                    mapOf("error" to "Not Found", "message" to "Produto nao encontrado"))
                return@get
            }

            call.logStatus(200)
            call.respond(HttpStatusCode.OK, product)
        }

        get("/{id}/reviews") {
            val id = call.parameters["id"] ?: ""

            val reviews = mutableListOf<Map<String, Any?>>()
            val rs = dbConnection.prepareStatement(
                "SELECT * FROM reviews WHERE product_id = ? ORDER BY created_at DESC"
            ).apply { setString(1, id) }.executeQuery()
            while (rs.next()) {
                reviews.add(mapOf(
                    "id" to rs.getInt("id"),
                    "product_id" to rs.getInt("product_id"),
                    "user_id" to rs.getInt("user_id"),
                    "user_name" to rs.getString("user_name"),
                    "rating" to rs.getInt("rating"),
                    "text" to rs.getString("text"),
                    "created_at" to rs.getString("created_at")
                ))
            }

            call.logSql("SELECT * FROM reviews WHERE product_id = $id")
            call.logStatus(200)
            call.logResponse("${reviews.size} reviews")
            call.respond(HttpStatusCode.OK, mapOf("reviews" to reviews, "count" to reviews.size))
        }

        post("/{id}/reviews") {
            val token = call.bearerToken()
            if (token == null) {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token nao fornecido"))
                return@post
            }

            val decoded = runCatching { JwtManager.verifier.verify(token) }.getOrElse {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token invalido ou expirado"))
                return@post
            }

            val userId = decoded.subject?.toInt() ?: 0
            val id = call.parameters["id"] ?: ""

            val body = call.receiveAndCacheText()
            val req = runCatching {
                json.decodeFromString(ReviewRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            if (req.text.isEmpty()) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Campo 'text' obrigatorio"))
                return@post
            }

            if (req.rating < 1 || req.rating > 5) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Rating deve ser entre 1 e 5"))
                return@post
            }

            val userRow = dbConnection.prepareStatement(
                "SELECT nome FROM users WHERE id = ?"
            ).apply { setInt(1, userId) }.executeQuery()
            val userName = if (userRow.next()) userRow.getString("nome") else "Usuario"

            dbConnection.prepareStatement(
                "INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)"
            ).apply {
                setString(1, id)
                setInt(2, userId)
                setString(3, userName)
                setInt(4, req.rating)
                setString(5, req.text)
                executeUpdate()
            }

            val review = mutableMapOf<String, Any?>()
            val rs = dbConnection.createStatement()
                .executeQuery("SELECT * FROM reviews WHERE rowid = last_insert_rowid()")
            if (rs.next()) {
                review["id"] = rs.getInt("id")
                review["product_id"] = rs.getInt("product_id")
                review["user_id"] = rs.getInt("user_id")
                review["user_name"] = rs.getString("user_name")
                review["rating"] = rs.getInt("rating")
                review["text"] = rs.getString("text")
                review["created_at"] = rs.getString("created_at")
            }

            call.logStatus(201)
            call.logResponse("review created")
            call.respond(HttpStatusCode.Created,
                mapOf("message" to "Review adicionada", "review" to review))
        }
    }
}
