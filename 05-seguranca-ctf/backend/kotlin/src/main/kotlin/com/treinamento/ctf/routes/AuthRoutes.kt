package com.treinamento.ctf.routes

import com.treinamento.ctf.auth.JwtManager
import com.treinamento.ctf.database.dbConnection
import com.treinamento.ctf.models.*
import com.treinamento.ctf.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun Route.authRoutes() {

    route("/api/auth") {

        post("/login") {
            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(LoginRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            val email = req.email
            val password = req.password

            val sqlQuery = "SELECT * FROM users WHERE email = '$email' AND password = '$password'"
            call.logSql(sqlQuery)

            val user: Map<String, Any?>?
            try {
                val rs = dbConnection.prepareStatement(
                    "SELECT * FROM users WHERE email = ? AND password = '$password'"
                ).apply { setString(1, email) }.executeQuery()

                user = if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "nome" to rs.getString("nome"),
                        "email" to rs.getString("email"),
                        "password" to rs.getString("password"),
                        "role" to rs.getString("role"),
                        "created_at" to rs.getString("created_at")
                    )
                } else null
            } catch (e: Exception) {
                call.logStatus(500)
                call.logResponse(e.message ?: "")
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@post
            }

            if (user == null) {
                call.logStatus(401)
                call.logResponse("Invalid credentials")
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Credenciais invalidas"))
                return@post
            }

            val token = JwtManager.generateToken(
                user["id"] as Int,
                user["email"] as String,
                user["role"] as String
            )

            call.logStatus(200)
            call.logResponse("""{"id":${user["id"]},"email":"${user["email"]}","role":"${user["role"]}"}""")
            call.respond(HttpStatusCode.OK, mapOf(
                "token" to token,
                "user" to mapOf(
                    "id" to user["id"],
                    "nome" to user["nome"],
                    "email" to user["email"],
                    "role" to user["role"]
                )
            ))
        }

        post("/register") {
            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(RegisterRequest.serializer(), body)
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

            val sqlQuery = "INSERT INTO users (nome, email, password, role) VALUES ('${req.nome}', '${req.email}', '${req.password}', '${req.role}')"
            call.logSql(sqlQuery)

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
                call.logResponse(e.message ?: "")
                call.respond(HttpStatusCode.Conflict,
                    mapOf("error" to "Conflict", "message" to "Email ja cadastrado"))
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
            call.respond(HttpStatusCode.Created,
                mapOf("message" to "Usuario criado com sucesso", "user" to newUser))
        }

        post("/forgot-password") {
            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(ForgotPasswordRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            if (req.email.isEmpty()) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Campo 'email' obrigatorio"))
                return@post
            }

            try {
                val smtpConfigPath = "config/smtp.json"
                val smtpConfig = java.io.File(smtpConfigPath).readText()
                val smtpConnection = smtpConfig

                val rs = dbConnection.prepareStatement(
                    "SELECT id, nome, email FROM users WHERE email = ?"
                ).apply { setString(1, req.email) }.executeQuery()

                if (rs.next()) {
                    val userId = rs.getInt("id")
                    val userEmail = rs.getString("email")
                    val resetToken = JwtManager.generateToken(userId, userEmail, "reset")
                    val resetUrl = "http://localhost:5173/reset-password?token=$resetToken"
                    println("[SMTP] Sending reset email to $userEmail")
                }

                call.logStatus(200)
                call.logSql("SELECT id, nome, email FROM users WHERE email = '${req.email}'")
                call.respond(HttpStatusCode.OK,
                    mapOf("message" to "Se o email estiver cadastrado, voce recebera as instrucoes de recuperacao."))
            } catch (e: Exception) {
                call.logStatus(500)
                call.logResponse(e.message ?: "")
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to "Internal Server Error",
                    "message" to (e.message ?: ""),
                    "stack" to e.stackTraceToString().lines().take(8)
                ))
            }
        }
    }
}
