package com.treinamento.ctf.routes

import com.treinamento.ctf.auth.*
import com.treinamento.ctf.database.dbConnection
import com.treinamento.ctf.database.reseedDatabase
import com.treinamento.ctf.models.*
import com.treinamento.ctf.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun Route.adminRoutes() {

    route("/api/admin") {

        get("/users") {
            val token = call.bearerToken()
            if (token == null) {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token nao fornecido"))
                return@get
            }

            val decoded = runCatching { JwtManager.verifier.verify(token) }.getOrElse {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token invalido ou expirado"))
                return@get
            }

            val role = decoded.getClaim("role").asString()
            if (role != "admin") {
                call.logStatus(403)
                call.logResponse("Forbidden")
                call.respond(HttpStatusCode.Forbidden,
                    mapOf("error" to "Forbidden", "message" to "Acesso negado"))
                return@get
            }

            val sqlQuery = "SELECT id, nome, email, password, role, created_at FROM users ORDER BY id"
            call.logSql(sqlQuery)

            val users = mutableListOf<Map<String, Any?>>()
            try {
                val rs = dbConnection.createStatement().executeQuery(sqlQuery)
                while (rs.next()) {
                    users.add(mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "email" to rs.getString("email"),
                        "password" to rs.getString("password"),
                        "role" to rs.getString("role"),
                        "created_at" to rs.getString("created_at")
                    ))
                }
            } catch (e: Exception) {
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            call.logStatus(200)
            call.logResponse("${users.size} users returned")
            call.respond(HttpStatusCode.OK, mapOf("users" to users, "count" to users.size))
        }

        post("/users") {
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

            val role = decoded.getClaim("role").asString()
            if (role != "admin") {
                call.logStatus(403)
                call.respond(HttpStatusCode.Forbidden,
                    mapOf("error" to "Forbidden", "message" to "Acesso negado"))
                return@post
            }

            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(AdminCreateUserRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            if (req.nome.isEmpty() || req.email.isEmpty() || req.password.isEmpty()) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Campos obrigatorios: nome, email, password"))
                return@post
            }

            try {
                dbConnection.prepareStatement(
                    "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, ?)"
                ).apply {
                    setString(1, req.nome)
                    setString(2, req.email)
                    setString(3, req.password)
                    setString(4, req.role)
                    executeUpdate()
                }
            } catch (e: Exception) {
                call.logStatus(409)
                call.respond(HttpStatusCode.Conflict,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@post
            }

            val newUser = runCatching {
                val rs = dbConnection.prepareStatement(
                    "SELECT id, nome, email, role FROM users WHERE email = ?"
                ).apply { setString(1, req.email) }.executeQuery()
                if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "email" to rs.getString("email"),
                        "role" to rs.getString("role")
                    )
                } else null
            }.getOrNull()

            call.logStatus(201)
            call.logSql("INSERT INTO users (nome, email, password, role) VALUES ('${req.nome}', '${req.email}', '***', '${req.role}')")
            call.respond(HttpStatusCode.Created,
                mapOf("message" to "Usuario criado com sucesso", "user" to newUser))
        }

        delete("/users/{id}") {
            val token = call.bearerToken()
            if (token == null) {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token nao fornecido"))
                return@delete
            }

            val decoded = runCatching { JwtManager.verifier.verify(token) }.getOrElse {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token invalido ou expirado"))
                return@delete
            }

            val role = decoded.getClaim("role").asString()
            if (role != "admin") {
                call.logStatus(403)
                call.respond(HttpStatusCode.Forbidden,
                    mapOf("error" to "Forbidden", "message" to "Acesso negado"))
                return@delete
            }

            val id = call.parameters["id"] ?: ""

            try {
                dbConnection.prepareStatement("DELETE FROM users WHERE id = ?")
                    .apply { setString(1, id); executeUpdate() }
            } catch (e: Exception) {
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@delete
            }

            call.logSql("DELETE FROM users WHERE id = $id")
            call.logStatus(200)
            call.logResponse("user $id deleted")
            call.respond(HttpStatusCode.OK,
                mapOf("message" to "Usuario $id removido com sucesso"))
        }

        post("/reset-db") {
            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(ResetDbRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            val RESET_KEY = "esales-ai-reset-2026"
            if (req.key != RESET_KEY) {
                call.logStatus(403)
                call.respond(HttpStatusCode.Forbidden,
                    mapOf("error" to "Forbidden", "message" to "Chave de reset invalida"))
                return@post
            }

            try {
                reseedDatabase()

                val userTotal = dbConnection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM users")
                    .also { it.next() }.getInt(1)

                call.logStatus(200)
                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "Database restaurado ao estado inicial",
                    "users" to userTotal,
                    "products" to 30,
                    "orders" to 50,
                    "reviews" to 5,
                    "coupons" to 3
                ))
            } catch (e: Exception) {
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Reset failed", "message" to (e.message ?: "")))
            }
        }
    }
}
