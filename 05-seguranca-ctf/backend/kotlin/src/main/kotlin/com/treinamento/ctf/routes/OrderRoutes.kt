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

fun Route.orderRoutes() {

    route("/api/orders") {

        get {
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
            val role = decoded.getClaim("role").asString()

            val sqlQuery: String
            val orders = mutableListOf<Map<String, Any?>>()

            if (role == "admin") {
                sqlQuery = "SELECT * FROM orders ORDER BY id DESC"
                call.logSql(sqlQuery)
                val rs = dbConnection.createStatement().executeQuery(sqlQuery)
                while (rs.next()) {
                    orders.add(mapOf(
                        "id" to rs.getInt("id"),
                        "user_id" to rs.getInt("user_id"),
                        "total" to rs.getDouble("total"),
                        "status" to rs.getString("status"),
                        "created_at" to rs.getString("created_at")
                    ))
                }
            } else {
                sqlQuery = "SELECT * FROM orders WHERE user_id = $userId ORDER BY id DESC"
                call.logSql(sqlQuery)
                val rs = dbConnection.prepareStatement(
                    "SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC"
                ).apply { setInt(1, userId) }.executeQuery()
                while (rs.next()) {
                    orders.add(mapOf(
                        "id" to rs.getInt("id"),
                        "user_id" to rs.getInt("user_id"),
                        "total" to rs.getDouble("total"),
                        "status" to rs.getString("status"),
                        "created_at" to rs.getString("created_at")
                    ))
                }
            }

            call.logStatus(200)
            call.logResponse("${orders.size} orders")
            call.respond(HttpStatusCode.OK, mapOf("orders" to orders, "count" to orders.size))
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

            val orderId = call.parameters["id"]?.toIntOrNull()
            if (orderId == null) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Invalid order id"))
                return@get
            }

            val sqlQuery = "SELECT o.*, u.nome as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = $orderId"
            call.logSql(sqlQuery)

            val order = runCatching {
                val rs = dbConnection.prepareStatement(
                    "SELECT o.*, u.nome as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?"
                ).apply { setInt(1, orderId) }.executeQuery()
                if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "user_id" to rs.getInt("user_id"),
                        "total" to rs.getDouble("total"),
                        "status" to rs.getString("status"),
                        "created_at" to rs.getString("created_at"),
                        "user_name" to rs.getString("user_name"),
                        "user_email" to rs.getString("user_email")
                    )
                } else null
            }.getOrElse { e ->
                call.logStatus(500)
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database error", "message" to (e.message ?: "")))
                return@get
            }

            if (order == null) {
                call.logStatus(404)
                call.respond(HttpStatusCode.NotFound,
                    mapOf("error" to "Not Found", "message" to "Pedido nao encontrado"))
                return@get
            }

            call.logStatus(200)
            call.logResponse("order $orderId")
            call.respond(HttpStatusCode.OK, order)
        }

        post {
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
            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(OrderCreateRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            if (req.items.isEmpty() || req.total <= 0) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Pedido deve conter itens e total valido"))
                return@post
            }

            val sqlQuery = "INSERT INTO orders (user_id, total, status) VALUES ($userId, ${req.total}, 'pendente')"
            call.logSql(sqlQuery)

            dbConnection.prepareStatement(
                "INSERT INTO orders (user_id, total, status) VALUES (?, ?, 'pendente')"
            ).apply {
                setInt(1, userId)
                setDouble(2, req.total)
                executeUpdate()
            }

            val newOrder = runCatching {
                val rs = dbConnection.createStatement()
                    .executeQuery("SELECT * FROM orders WHERE rowid = last_insert_rowid()")
                if (rs.next()) {
                    mapOf(
                        "id" to rs.getInt("id"),
                        "user_id" to rs.getInt("user_id"),
                        "total" to rs.getDouble("total"),
                        "status" to rs.getString("status"),
                        "created_at" to rs.getString("created_at")
                    )
                } else null
            }.getOrNull()

            call.logStatus(201)
            call.logResponse("order ${(newOrder?.get("id") ?: "?")} created")
            call.respond(HttpStatusCode.Created,
                mapOf("message" to "Pedido criado com sucesso", "order" to newOrder))
        }

        post("/{id}/apply-coupon") {
            val token = call.bearerToken()
            if (token == null) {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token nao fornecido"))
                return@post
            }

            runCatching { JwtManager.verifier.verify(token) }.getOrElse {
                call.logStatus(401)
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token invalido ou expirado"))
                return@post
            }

            val orderId = call.parameters["id"] ?: ""
            val body = call.receiveAndCacheText()

            val req = runCatching {
                json.decodeFromString(CouponApplyRequest.serializer(), body)
            }.getOrElse {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "JSON invalido"))
                return@post
            }

            if (req.code.isEmpty()) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Codigo do cupom obrigatorio"))
                return@post
            }

            val couponRs = dbConnection.prepareStatement(
                "SELECT * FROM coupons WHERE code = ? AND active = 1"
            ).apply { setString(1, req.code) }.executeQuery()

            if (!couponRs.next()) {
                call.logStatus(404)
                call.respond(HttpStatusCode.NotFound,
                    mapOf("error" to "Not Found", "message" to "Cupom nao encontrado"))
                return@post
            }

            val couponId = couponRs.getInt("id")
            val couponDiscount = couponRs.getDouble("discount")
            val couponMaxUses = couponRs.getInt("max_uses")
            val couponUses = couponRs.getInt("uses")

            if (couponUses >= couponMaxUses) {
                call.logStatus(400)
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to "Bad Request", "message" to "Cupom esgotado"))
                return@post
            }

            Thread.sleep(100)

            dbConnection.prepareStatement(
                "UPDATE coupons SET uses = uses + 1 WHERE id = ?"
            ).apply { setInt(1, couponId); executeUpdate() }

            val orderRs = dbConnection.prepareStatement(
                "SELECT * FROM orders WHERE id = ?"
            ).apply { setString(1, orderId) }.executeQuery()

            if (!orderRs.next()) {
                call.logStatus(404)
                call.respond(HttpStatusCode.NotFound,
                    mapOf("error" to "Not Found", "message" to "Pedido nao encontrado"))
                return@post
            }

            val orderTotal = orderRs.getDouble("total")
            val discount = Math.round(orderTotal * (couponDiscount / 100) * 100) / 100.0
            val newTotal = Math.round((orderTotal - discount) * 100) / 100.0

            dbConnection.prepareStatement(
                "UPDATE orders SET total = ? WHERE id = ?"
            ).apply { setDouble(1, newTotal); setString(2, orderId); executeUpdate() }

            call.logStatus(200)
            call.logSql("UPDATE coupons SET uses = uses + 1 WHERE id = $couponId; UPDATE orders SET total = $newTotal WHERE id = $orderId")
            call.logResponse("coupon ${req.code} applied, discount $discount")
            call.respond(HttpStatusCode.OK, mapOf(
                "message" to "Cupom aplicado com sucesso",
                "discount" to discount,
                "original_total" to orderTotal,
                "new_total" to newTotal,
                "coupon_code" to req.code,
                "coupon_uses" to couponUses + 1,
                "coupon_max" to couponMaxUses
            ))
        }
    }
}
