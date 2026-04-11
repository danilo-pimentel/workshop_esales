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

fun Route.userRoutes() {

    route("/api/users") {

        get("/me") {
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

            val userId = decoded.subject?.toInt() ?: 0
            val sqlQuery = "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = $userId"
            call.logSql(sqlQuery)

            val user = runCatching {
                val rs = dbConnection.prepareStatement(
                    "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?"
                ).apply { setInt(1, userId) }.executeQuery()
                if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "email" to rs.getString("email"),
                        "role" to rs.getString("role"),
                        "telefone" to rs.getString("telefone"),
                        "cpf_last4" to rs.getString("cpf_last4"),
                        "endereco" to rs.getString("endereco"),
                        "created_at" to rs.getString("created_at")
                    )
                } else null
            }.getOrElse { e ->
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            if (user == null) {
                call.logStatus(404)
                call.respond(HttpStatusCode.NotFound,
                    mapOf("error" to "Not Found", "message" to "Usuario nao encontrado"))
                return@get
            }

            call.logStatus(200)
            call.respond(HttpStatusCode.OK, user)
        }

        get("/{id}") {
            val token = call.bearerToken()
            if (token == null) {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token nao fornecido"))
                return@get
            }

            runCatching { JwtManager.verifier.verify(token) }.getOrElse {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token invalido ou expirado"))
                return@get
            }

            val requestedId = call.parameters["id"]?.toIntOrNull()
            if (requestedId == null) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Invalid user id"))
                return@get
            }

            val sqlQuery = "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = $requestedId"
            call.logSql(sqlQuery)

            val user: Map<String, Any?>?
            try {
                val rs = dbConnection.prepareStatement(
                    "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?"
                ).apply { setInt(1, requestedId) }.executeQuery()
                user = if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "email" to rs.getString("email"),
                        "role" to rs.getString("role"),
                        "telefone" to rs.getString("telefone"),
                        "cpf_last4" to rs.getString("cpf_last4"),
                        "endereco" to rs.getString("endereco"),
                        "created_at" to rs.getString("created_at")
                    )
                } else null
            } catch (e: Exception) {
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            if (user == null) {
                call.logStatus(404)
                call.respond(HttpStatusCode.NotFound,
                    mapOf("error" to "Not Found", "message" to "Usuario nao encontrado"))
                return@get
            }

            call.logStatus(200)
            call.logResponse("user $requestedId")
            call.respond(HttpStatusCode.OK, user)
        }

        put("/{id}") {
            val token = call.bearerToken()
            if (token == null) {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token nao fornecido"))
                return@put
            }

            val decoded = runCatching { JwtManager.verifier.verify(token) }.getOrElse {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token invalido ou expirado"))
                return@put
            }

            val tokenUserId = decoded.subject?.toInt() ?: 0
            val tokenRole = decoded.getClaim("role").asString()
            val id = call.parameters["id"] ?: ""

            if (id != tokenUserId.toString() && tokenRole != "admin") {
                call.logStatus(403)
                call.respond(HttpStatusCode.Forbidden,
                    mapOf("error" to "Forbidden", "message" to "Acesso negado"))
                return@put
            }

            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(UpdateUserRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@put
            }

            try {
                dbConnection.prepareStatement("""
                    UPDATE users SET
                        nome     = COALESCE(?, nome),
                        email    = COALESCE(?, email),
                        password = COALESCE(?, password),
                        telefone = COALESCE(?, telefone),
                        endereco = COALESCE(?, endereco)
                    WHERE id = ?
                """.trimIndent()).apply {
                    setString(1, req.nome)
                    setString(2, req.email)
                    setString(3, req.password)
                    setString(4, req.telefone)
                    setString(5, req.endereco)
                    setString(6, id)
                    executeUpdate()
                }
            } catch (e: Exception) {
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@put
            }

            val updated = runCatching {
                val rs = dbConnection.prepareStatement(
                    "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?"
                ).apply { setString(1, id) }.executeQuery()
                if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "email" to rs.getString("email"),
                        "role" to rs.getString("role"),
                        "telefone" to rs.getString("telefone"),
                        "cpf_last4" to rs.getString("cpf_last4"),
                        "endereco" to rs.getString("endereco"),
                        "created_at" to rs.getString("created_at")
                    )
                } else null
            }.getOrNull()

            call.logStatus(200)
            call.logSql("UPDATE users SET ... WHERE id = $id")
            call.logResponse("Updated")
            call.respond(HttpStatusCode.OK,
                mapOf("message" to "Usuario atualizado", "user" to updated))
        }
    }
}
