package com.treinamento.ctf

import com.fasterxml.jackson.databind.SerializationFeature
import com.treinamento.ctf.database.initDatabase
import com.treinamento.ctf.plugins.RequestLoggerPlugin
import com.treinamento.ctf.plugins.configureCors
import com.treinamento.ctf.routes.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    val dbPath = System.getenv("SECURESHOP_DB_PATH") ?: "secureshop.db"
    initDatabase(dbPath)

    embeddedServer(
        Netty,
        port = 4000,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureCors(this)

    // Ensure CORS headers on every response
    intercept(ApplicationCallPipeline.Plugins) {
        call.response.headers.append("Access-Control-Allow-Origin", "*")
        call.response.headers.append("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
        call.response.headers.append("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Role")
    }

    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(RequestLoggerPlugin)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Internal Server Error",
                    "message" to (cause.message ?: "Unknown error"),
                    "stack" to cause.stackTraceToString().lines().take(8)
                )
            )
        }
    }

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "port" to 4000))
        }

        authRoutes()
        userRoutes()
        productRoutes()
        orderRoutes()
        adminRoutes()
        logRoutes()
        exportRoutes()

        staticResources("/monitor", "static/monitor") {
            default("index.html")
        }

        get("/") {
            call.respond(HttpStatusCode.OK, mapOf(
                "service" to "SecureShop API",
                "version" to "1.0.0",
                "status" to "running",
                "endpoints" to mapOf(
                    "auth" to "/api/auth/login  /api/auth/register  /api/auth/forgot-password",
                    "users" to "/api/users/me  /api/users/:id",
                    "products" to "/api/products  /api/products/search?q=  /api/products/:id/reviews",
                    "orders" to "/api/orders  /api/orders/:id  /api/orders/:id/apply-coupon",
                    "export" to "/api/export/:format",
                    "admin" to "/api/admin/users",
                    "logs" to "/api/logs",
                    "monitor" to "/monitor"
                )
            ))
        }
    }
}
