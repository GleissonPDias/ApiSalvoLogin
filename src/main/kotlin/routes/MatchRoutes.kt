package com.example.routes

import com.example.database.solicitarSocorroRadar
import com.example.models.PedidoSocorroRequest
import com.example.models.PedidoSocorroResponse
import com.example.database.buscarPedidos
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.matchRoutes() {
    post("/solicitar-socorro") {
        try {
            val pedido = call.receive<PedidoSocorroRequest>()
            val resposta = solicitarSocorroRadar(pedido)

            if (resposta.sucesso) {
                call.respond(HttpStatusCode.Created, resposta)
            } else {
                call.respond(HttpStatusCode.InternalServerError, resposta)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, PedidoSocorroResponse(false, "Erro na requisição: ${e.message}"))
        }
    }
}






