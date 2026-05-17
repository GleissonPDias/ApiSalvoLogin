package com.example.routes

import com.example.database.solicitarSocorroRadar
import com.example.database.DatabaseConfig
import com.example.models.PedidoSocorroRequest
import com.example.models.PedidoSocorroResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

fun Route.matchRoutes() {
    post("/solicitar-socorro") {
        try {
            val pedido = call.receive<PedidoSocorroRequest>()
            val resposta = solicitarSocorroRadar(pedido)

            if (resposta.sucesso) {
                val idGerado = resposta.requestId

                // ==============================================================
                // ⏱️ RELÓGIO DA BOMBA DE TEMPO (3 MINUTOS)
                // ==============================================================
                if (idGerado != null) {
                    launch {
                        delay(3 * 60 * 1000L)
                        try {
                            DatabaseConfig.getConnection().use { conn ->
                                val sqlCancel = """
                                    UPDATE service_requests 
                                    SET status = 'canceled', cancellation_reason = 'timeout_no_provider' 
                                    WHERE id = ? AND status = 'searching'
                                """.trimIndent()
                                val stmtCancel = conn.prepareStatement(sqlCancel)
                                stmtCancel.setInt(1, idGerado)
                                val afetados = stmtCancel.executeUpdate()
                                if (afetados > 0) println("⏳ Timeout! Pedido $idGerado cancelado.")
                            }
                        } catch (e: Exception) {
                            println("Erro no timeout: ${e.message}")
                        }
                    }
                }
                // ==============================================================

                // 🚀 WEBSOCKET: ENVIO DINÂMICO PARA CADA OFICINA
                val matches = resposta.prestadoresMatch ?: emptyList()

                for (oficina in matches) {
                    val sessaoMecanico = prestadoresConectados[oficina.providerId]

                    if (sessaoMecanico != null) {
                        // JSON Customizado para ESTA oficina específica
                        val dadosDoChamadoJson = """
                            {
                                "requestId": ${idGerado},
                                "rawPreco": ${oficina.preco},
                                "rawDistancia": ${oficina.distanciaKm},
                                "veiculo": "Solicitação de ${pedido.serviceType}",
                                "defeito": "🔧 ${pedido.description}",
                                "preco": "R$ ${String.format("%.2f", oficina.preco).replace(".", ",")}",
                                "distanciaText": "Distância: ${oficina.distanciaKm} km  •  ~${oficina.minutosEstimados} min",
                                "clienteNome": "Cliente ID: ${pedido.customerId}",
                                "clienteNota": "⭐ 4.9 (Nova solicitação)"
                            }
                        """.trimIndent()

                        try {
                            sessaoMecanico.send(Frame.Text(dadosDoChamadoJson))
                            println("📱 Alerta Dinâmico enviado via WebSocket para a oficina ID: ${oficina.providerId}")
                        } catch (e: Exception) {
                            println("⚠️ Falha ao enviar alerta para a oficina ${oficina.providerId}.")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, resposta)
            } else {
                call.respond(HttpStatusCode.InternalServerError, resposta)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, PedidoSocorroResponse(false, "Erro: ${e.message}"))
        }
    }

    post("/aceitar-socorro") {
        try {
            val dados = call.receive<com.example.models.AceitarPedidoRequest>()
            val sucesso = com.example.database.aceitarPedidoBanco(dados)

            if (sucesso) {
                call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Corrida aceita com sucesso!"))
            } else {
                call.respond(HttpStatusCode.Conflict, mapOf("sucesso" to false, "mensagem" to "Ops! Outro prestador já aceitou essa chamada."))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Erro: ${e.message}"))
        }
    }


}