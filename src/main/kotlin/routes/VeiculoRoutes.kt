package com.example.routes

import com.example.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.veiculoRoutes() {

    // LER: Retorna todos os veículos ativos da oficina
    get("/veiculos-oficina/{providerId}") {
        val providerId = call.parameters["providerId"]?.toIntOrNull()
        if (providerId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "ID da oficina inválido"))
            return@get
        }
        val veiculos = buscarVeiculosDaOficina(providerId)
        call.respond(HttpStatusCode.OK, veiculos)
    }

    // CRIAR: Cadastra um novo guincho/moto
    post("/adicionar-veiculo") {
        val campos = call.receive<Map<String, String?>>()
        val providerId = campos["provider_id"]?.toIntOrNull()
        val name = campos["name"]
        val plate = campos["plate"]
        val status = campos["status"] ?: "Disponível"
        val photoBase64 = campos["vehicle_photo"]

        if (providerId == null || name.isNullOrBlank() || plate.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Campos obrigatórios ausentes (provider_id, name, plate)"))
            return@post
        }

        val sucesso = adicionarVeiculoNoBanco(providerId, name, plate, status, photoBase64)
        if (sucesso) {
            call.respond(HttpStatusCode.Created, mapOf("sucesso" to true, "mensagem" to "Veículo cadastrado com sucesso!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao salvar veículo no banco"))
        }
    }

    // ATUALIZAR: Muda o status (Ex: de 'Disponível' para 'Em atendimento')
    patch("/atualizar-status-veiculo/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val campos = call.receive<Map<String, String>>()
        val providerId = campos["provider_id"]?.toIntOrNull()
        val novoStatus = campos["status"]

        if (id == null || providerId == null || novoStatus.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Parâmetros inválidos"))
            return@patch
        }

        val sucesso = atualizarStatusVeiculoNoBanco(id, providerId, novoStatus)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Status atualizado com sucesso!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao atualizar status"))
        }
    }

    // EXCLUIR: Remove o veículo da lista
    delete("/excluir-veiculo/{id}/{providerId}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val providerId = call.parameters["providerId"]?.toIntOrNull()

        if (id == null || providerId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "IDs de exclusão inválidos"))
            return@delete
        }

        val sucesso = excluirVeiculoNoBanco(id, providerId)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Veículo excluído com sucesso!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao excluir veículo"))
        }
    }

    put("/atualizar-veiculo/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val campos = call.receive<Map<String, String?>>()
        val providerId = campos["provider_id"]?.toIntOrNull()
        val name = campos["name"]
        val plate = campos["plate"]
        val photoBase64 = campos["vehicle_photo"]

        if (id == null || providerId == null || name.isNullOrBlank() || plate.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Dados incompletos"))
            return@put
        }

        val sucesso = atualizarDadosVeiculoNoBanco(id, providerId, name, plate, photoBase64)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Veículo atualizado!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao atualizar"))
        }
    }
}

