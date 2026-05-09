package com.example.routes

import com.example.database.buscarPedidos
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlin.text.toIntOrNull

fun Route.pedidoRoutes() {
    get("/listar-pedidos") {
        try {
            // Pega o userId dos parâmetros da URL (?userId=1)
            val userId = call.request.queryParameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("erro" to "ID do usuário não fornecido"))
                return@get
            }

            val pedidos = buscarPedidos(userId)

            // Retorna a lista de pedidos encontrada
            call.respond(HttpStatusCode.OK, pedidos)

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar pedidos: ${e.message}")
        }
    }
}