package com.example.routes

import com.example.database.buscarHistoricoDaOficina
import com.example.database.buscarPedidos
import com.example.database.verificarStatusDoPedidoBanco // <-- NÃO ESQUEÇA DESTE IMPORT
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlin.text.toIntOrNull

fun Route.pedidoRoutes() {

    // ROTA QUE VOCÊ JÁ TINHA
    get("/listar-pedidos") {
        try {
            val userId = call.request.queryParameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("erro" to "ID do usuário não fornecido"))
                return@get
            }

            val pedidos = buscarPedidos(userId)
            call.respond(HttpStatusCode.OK, pedidos)

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Erro ao buscar pedidos: ${e.message}")
        }
    }

    // ==============================================================
    // 🔄 NOVA ROTA: O POLLING DO APLICATIVO DO CLIENTE
    // ==============================================================
    get("/status-pedido/{id}") {
        val requestId = call.parameters["id"]?.toIntOrNull()
        if (requestId != null) {
            val statusAtualizado = verificarStatusDoPedidoBanco(requestId)
            call.respond(HttpStatusCode.OK, statusAtualizado)
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to "ID inválido"))
        }
    }
    get("/listar-pedidos-oficina") {
        val providerId = call.request.queryParameters["providerId"]?.toIntOrNull()
        if (providerId != null) {
            val pedidos = buscarHistoricoDaOficina(providerId)
            call.respond(pedidos)
        } else {
            call.respond(HttpStatusCode.BadRequest, "ID do prestador ausente.")
        }
    }
}