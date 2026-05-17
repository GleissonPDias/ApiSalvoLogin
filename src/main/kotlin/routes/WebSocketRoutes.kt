package com.example.routes

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

// Banco de dados em memória para gerenciar quais oficinas estão online no Socket agora
val prestadoresConectados = ConcurrentHashMap<Int, DefaultWebSocketServerSession>()

fun Route.radarWebSocketRoute() {
    webSocket("/radar-provider/{id}") {
        val providerId = call.parameters["id"]?.toIntOrNull()

        if (providerId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "ID do Prestador inválido"))
            return@webSocket
        }

        // Adiciona a sessão do aparelho à lista
        prestadoresConectados[providerId] = this
        println("🚀 Oficina ID $providerId conectada com sucesso ao canal WebSocket!")

        try {
            // Loop que mantém o "tunel" de comunicação aberto
            for (frame in incoming) {
                // O app do prestador não precisa enviar texto, apenas receber chamados.
            }
        } catch (e: Exception) {
            println("Conexão WebSocket da oficina $providerId caiu: ${e.message}")
        } finally {
            // Remove a oficina se ela fechar o app ou o Switch mudar para offline
            prestadoresConectados.remove(providerId)
            println("Oficina ID $providerId desconectada do canal WebSocket.")
        }
    }
}