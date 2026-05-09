package com.example.routes

import com.example.database.buscarPerfilNoBanco
import com.example.database.atualizarPerfilNoBanco
import com.example.database.buscarServicosDaOficina
import com.example.models.AuthResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.perfilRoutes() {

    // ROTA PARA BUSCAR DADOS (GET)
    get("/obter-perfil/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "ID inválido"))
            return@get
        }

        val dados = buscarPerfilNoBanco(id)
        if (dados != null) {
            call.respond(HttpStatusCode.OK, dados)
        } else {
            call.respond(HttpStatusCode.NotFound, AuthResponse(false, "Perfil não encontrado"))
        }
    }

    // ROTA PARA ATUALIZAR DADOS (PATCH)
    patch("/atualizar-perfil/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val campos = call.receive<Map<String, String>>()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "ID inválido"))
            return@patch
        }

        val sucesso = atualizarPerfilNoBanco(id, campos)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, AuthResponse(true, "Alteração salva com sucesso!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, AuthResponse(false, "Erro ao atualizar banco"))
        }
    }

    get("/servicos-oficina/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "ID inválido")
            return@get
        }

        val servicos = buscarServicosDaOficina(id)
        call.respond(HttpStatusCode.OK, servicos)
    }
}

