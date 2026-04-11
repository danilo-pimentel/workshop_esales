package com.treinamento.ctf.routes

import com.treinamento.ctf.auth.*
import com.treinamento.ctf.database.dbConnection
import com.treinamento.ctf.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.logRoutes() {

    get("/api/logs") {
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
            call.respond(HttpStatusCode.Forbidden,
                mapOf("error" to "Forbidden", "message" to "Acesso negado"))
            return@get
        }

        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

        val logs = mutableListOf<Map<String, Any?>>()
        runCatching {
            val rs = dbConnection.prepareStatement(
                "SELECT * FROM request_logs ORDER BY id DESC LIMIT ?"
            ).apply { setInt(1, limit) }.executeQuery()
            while (rs.next()) {
                logs.add(mapOf(
                    "id" to rs.getInt("id"),
                    "method" to rs.getString("method"),
                    "path" to rs.getString("path"),
                    "query_params" to rs.getString("query_params"),
                    "body" to rs.getString("body"),
                    "status_code" to rs.getInt("status_code"),
                    "sql_query" to rs.getString("sql_query"),
                    "response_preview" to rs.getString("response_preview"),
                    "ip" to rs.getString("ip"),
                    "created_at" to rs.getString("created_at")
                ))
            }
        }.onFailure { e ->
            call.logStatus(500)
            call.respond(HttpStatusCode.InternalServerError,
                mapOf("error" to "Database error", "message" to (e.message ?: "")))
            return@get
        }

        call.logStatus(200)
        call.logSql("SELECT * FROM request_logs ORDER BY id DESC LIMIT $limit")
        call.logResponse("${logs.size} log entries returned")
        call.respond(HttpStatusCode.OK, mapOf("logs" to logs, "count" to logs.size))
    }

    delete("/api/logs") {
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

        dbConnection.createStatement().executeUpdate("DELETE FROM request_logs")

        call.logStatus(200)
        call.logSql("DELETE FROM request_logs")
        call.logResponse("All logs cleared")
        call.respond(HttpStatusCode.OK,
            mapOf("message" to "Todos os logs foram apagados"))
    }
}
