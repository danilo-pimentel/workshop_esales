package com.treinamento.ctf.plugins

import com.treinamento.ctf.database.dbConnection
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.plugins.origin
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RequestLogger")

val AttributeKeySql: AttributeKey<String> = AttributeKey("executedSql")
val AttributeKeyBody: AttributeKey<String> = AttributeKey("rawBody")
val AttributeKeyStatusCode: AttributeKey<Int> = AttributeKey("logStatusCode")
val AttributeKeyResponsePreview: AttributeKey<String> = AttributeKey("logResponsePreview")

val RequestLoggerPlugin = createApplicationPlugin(name = "RequestLoggerPlugin") {

    onCall { call ->
        call.attributes.put(AttributeKeyBody, "")
        call.attributes.put(AttributeKeySql, "")
        call.attributes.put(AttributeKeyStatusCode, 0)
        call.attributes.put(AttributeKeyResponsePreview, "")
    }

    onCallRespond { call, _ ->
        val sql         = runCatching { call.attributes[AttributeKeySql] }.getOrDefault("")
        val body        = runCatching { call.attributes[AttributeKeyBody] }.getOrDefault("")
        val statusCode  = runCatching { call.attributes[AttributeKeyStatusCode] }.getOrDefault(0)
        val respPreview = runCatching { call.attributes[AttributeKeyResponsePreview] }.getOrDefault("")
        val queryParams = call.request.queryString().takeIf { it.isNotEmpty() }
        val path        = call.request.uri.substringBefore("?")
        val method      = call.request.httpMethod.value
        val ip          = call.request.origin.remoteHost

        val statusColor = when {
            statusCode >= 500 -> "\u001b[31m"
            statusCode >= 400 -> "\u001b[33m"
            else -> "\u001b[32m"
        }
        val reset = "\u001b[0m"
        val dim = "\u001b[2m"
        println("$dim${java.time.Instant.now()}$reset  $statusColor${statusCode}$reset  ${method.padEnd(6)} $path${if (queryParams != null) "?$queryParams" else ""}${if (body.isNotEmpty()) "$dim  body=${body.take(80)}$reset" else ""}")

        withContext(Dispatchers.IO) {
            runCatching {
                dbConnection.prepareStatement("""
                    INSERT INTO request_logs
                        (method, path, query_params, body, status_code, sql_query, response_preview, ip)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).apply {
                    setString(1, method)
                    setString(2, path)
                    setString(3, queryParams)
                    setString(4, body.take(1000))
                    setInt(5, statusCode)
                    setString(6, sql)
                    setString(7, respPreview.take(500))
                    setString(8, ip)
                    executeUpdate()
                }
            }.onFailure { e ->
                logger.warn("[Logger] Failed to persist request log: ${e.message}")
            }
        }
    }
}

fun ApplicationCall.logSql(sql: String) {
    runCatching { attributes.put(AttributeKeySql, sql) }
}

fun ApplicationCall.logStatus(code: Int) {
    runCatching { attributes.put(AttributeKeyStatusCode, code) }
}

fun ApplicationCall.logResponse(preview: String) {
    runCatching { attributes.put(AttributeKeyResponsePreview, preview) }
}

suspend fun ApplicationCall.receiveAndCacheText(): String {
    val body: String = runCatching { receiveText() }.getOrDefault("")
    runCatching { attributes.put(AttributeKeyBody, body) }
    return body
}
