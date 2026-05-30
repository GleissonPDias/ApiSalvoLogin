package com.example.routes

import com.example.database.atualizarStatusPedidoBanco
import com.example.database.buscarHistoricoDaOficina
import com.example.database.buscarPedidos
import com.example.database.verificarStatusDoPedidoBanco // <-- NÃO ESQUEÇA DESTE IMPORT
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
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

    patch("/atualizar-status-pedido/{id}") {
        try {
            val pedidoId = call.parameters["id"]?.toIntOrNull()
            val campos = call.receive<Map<String, String>>()

            val providerId = campos["provider_id"]?.toIntOrNull()
            val novoStatus = campos["status"]

            if (pedidoId == null || providerId == null || novoStatus.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Parâmetros inválidos"))
                return@patch
            }

            val sucesso = atualizarStatusPedidoBanco(pedidoId, providerId, novoStatus)

            if (sucesso) {
                call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Status atualizado com sucesso!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao atualizar status no banco."))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Erro: ${e.message}"))
        }
    }
}