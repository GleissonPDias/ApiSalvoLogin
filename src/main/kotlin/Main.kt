package com.example


import com.example.routes.authRoutes
import com.example.routes.matchRoutes
import com.example.routes.pedidoRoutes
import com.example.routes.perfilRoutes
import com.example.routes.servicoRoutes
import com.example.routes.veiculoRoutes
import com.example.routes.radarWebSocketRoute
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*



fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {

        install(WebSockets)

        install(ContentNegotiation) {
            gson { setPrettyPrinting() }
        }

        routing {
            // Chamando as rotas que foram divididas nas outras pastas
            authRoutes()
            matchRoutes()
            pedidoRoutes()
            perfilRoutes()
            veiculoRoutes()
            servicoRoutes()

            radarWebSocketRoute()

        }

    }.start(wait = true)
}