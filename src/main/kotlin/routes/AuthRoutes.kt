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
import com.example.database.solicitarRecuperacao
import com.example.models.ResetPasswordRequest
import com.example.models.GenericResponse

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

    // NOVO: Rota para redefinição de senha
    post("/recuperar-senha") {
        try {
            // Recebe o JSON do Android { "email": "teste@teste.com" }
            val pedido = call.receive<ResetPasswordRequest>()

            // Vai no banco checar
            val resposta = solicitarRecuperacao(pedido.email)

            // Retornamos OK (Status 200) para o Android ler a mensagem facilmente.
            // Se o sucesso for false, o Android vai ver isso no JSON.
            call.respond(HttpStatusCode.OK, resposta)

        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "Erro na API: ${e.message}"))
        }
    }
}