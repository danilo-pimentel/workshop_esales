package com.treinamento.ctf.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.Date

object JwtManager {

    const val SECRET = "super-secret-jwt-key-2024"

    private val algorithm = Algorithm.HMAC256(SECRET)

    val verifier: JWTVerifier = JWT.require(algorithm).build()

    fun generateToken(userId: Int, email: String, role: String): String =
        JWT.create()
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L))
            .sign(algorithm)

    fun getUserId(token: String): Int? = runCatching {
        JWT.decode(token).subject?.toInt()
    }.getOrNull()

    fun getRole(token: String): String? = runCatching {
        JWT.decode(token).getClaim("role").asString()
    }.getOrNull()

    fun getEmail(token: String): String? = runCatching {
        JWT.decode(token).getClaim("email").asString()
    }.getOrNull()
}

fun ApplicationCall.bearerToken(): String? =
    request.headers["Authorization"]
        ?.removePrefix("Bearer ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

data class TokenPayload(val sub: Int, val email: String, val role: String)

fun ApplicationCall.requireAuth(): TokenPayload? {
    val token = bearerToken() ?: return null
    return runCatching {
        val decoded = JwtManager.verifier.verify(token)
        TokenPayload(
            sub = decoded.subject.toInt(),
            email = decoded.getClaim("email").asString(),
            role = decoded.getClaim("role").asString()
        )
    }.getOrNull()
}
