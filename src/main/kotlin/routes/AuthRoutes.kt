package com.example.routes

import com.example.database.cadastrarNoBanco
import com.example.database.validarNoBanco
import com.example.models.AuthResponse
import com.example.models.LoginRequest
import com.example.models.RegisterRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    post("/login") {
        try {
            val pedido = call.receive<LoginRequest>()
            val resposta = validarNoBanco(pedido.email, pedido.password)

            if (resposta.sucesso) {
                call.respond(HttpStatusCode.OK, resposta)
            } else {
                call.respond(HttpStatusCode.Unauthorized, resposta)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "Erro na API: ${e.message}"))
        }
    }

    post("/cadastro") {
        try {
            val pedido = call.receive<RegisterRequest>()
            val resposta = cadastrarNoBanco(pedido)

            if (resposta.sucesso) {
                call.respond(HttpStatusCode.Created, resposta)
            } else {
                call.respond(HttpStatusCode.BadRequest, resposta)
            }
        } catch (e: Exception) {
            println("ERRO NO CADASTRO: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "Erro na API: ${e.message}"))
        }
    }
}