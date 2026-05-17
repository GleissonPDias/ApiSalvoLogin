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
import io.ktor.websocket.* // Necessário para o envio dos Frames

fun Route.matchRoutes() {
    post("/solicitar-socorro") {
        try {
            val pedido = call.receive<PedidoSocorroRequest>()
            val resposta = solicitarSocorroRadar(pedido)

            if (resposta.sucesso) {

                // =================================================================
                // 🚀 SISTEMA DE DISPARO WEBSOCKET EM TEMPO REAL
                // =================================================================

                // 1. Monta o JSON com os campos exatos que o Android espera ler
                val dadosDoChamadoJson = """
                    {
                        "veiculo": "Solicitação de ${pedido.serviceType}",
                        "defeito": "🔧 ${pedido.description}",
                        "preco": "R$ 150,00",
                        "clienteNome": "Cliente ID: ${pedido.customerId}",
                        "clienteNota": "⭐ 4.9 (Nova viagem)"
                    }
                """.trimIndent()

                // 2. Recupera a lista de IDs de quem estava dentro do raio do GPS
                val oficinasNoRaio = resposta.idsPrestadores ?: emptyList()

                // 3. Varre a lista e entrega o alerta instantaneamente na tela de quem está online
                for (providerId in oficinasNoRaio) {
                    val sessaoMecanico = prestadoresConectados[providerId]
                    if (sessaoMecanico != null) {
                        try {
                            sessaoMecanico.send(Frame.Text(dadosDoChamadoJson))
                            println("📱 Alerta de socorro enviado via WebSocket para a oficina ID: $providerId")
                        } catch (e: Exception) {
                            println("⚠️ Canal instável para a oficina $providerId, falha no envio.")
                        }
                    }
                }
                // =================================================================

                call.respond(HttpStatusCode.Created, resposta)
            } else {
                call.respond(HttpStatusCode.InternalServerError, resposta)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, PedidoSocorroResponse(false, "Erro na requisição: ${e.message}"))
        }
    }
}