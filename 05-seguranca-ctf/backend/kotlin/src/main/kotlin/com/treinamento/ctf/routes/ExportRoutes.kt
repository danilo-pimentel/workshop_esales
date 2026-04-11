package com.treinamento.ctf.routes

import com.treinamento.ctf.auth.*
import com.treinamento.ctf.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.exportRoutes() {

    get("/api/export/{format}") {
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

        val format = call.parameters["format"] ?: ""

        val templatePath = "templates/$format"
        val templateFile = File(templatePath)

        val template: String
        try {
            template = templateFile.readText()
        } catch (e: Exception) {
            call.logStatus(404)
            call.respond(HttpStatusCode.NotFound,
                mapOf("error" to "Not Found", "message" to "Template '$format' nao encontrado"))
            return@get
        }

        call.logStatus(200)
        call.logResponse("Template $format loaded (${template.length} bytes)")
        call.respond(HttpStatusCode.OK, mapOf(
            "format" to format,
            "template" to template,
            "message" to "Template carregado"
        ))
    }
}
