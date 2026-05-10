package com.example.routes

import com.example.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.servicoRoutes() {

    // GET: Lista todos os serviços cadastrados pela oficina
    get("/servicos-oficina/{providerId}") {
        val providerId = call.parameters["providerId"]?.toIntOrNull()
        if (providerId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "ID inválido"))
            return@get
        }
        val servicos = buscarServicosDaOficina(providerId)
        call.respond(HttpStatusCode.OK, servicos)
    }

    // POST: Cadastra um novo serviço com preço base e preço por km
    post("/adicionar-servico") {
        val campos = call.receive<Map<String, String>>()
        val providerId = campos["provider_id"]?.toIntOrNull()
        val serviceType = campos["service_type"]
        val basePrice = campos["base_price"]?.toDoubleOrNull() ?: 0.0
        val pricePerKm = campos["price_per_km"]?.toDoubleOrNull() ?: 0.0

        if (providerId == null || serviceType.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Campos obrigatórios ausentes"))
            return@post
        }

        val sucesso = adicionarServicoNoBanco(providerId, serviceType, basePrice, pricePerKm)
        if (sucesso) {
            call.respond(HttpStatusCode.Created, mapOf("sucesso" to true, "mensagem" to "Serviço cadastrado!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao cadastrar"))
        }
    }

    // PUT: Atualiza os preços ou o nome do serviço
    put("/atualizar-servico/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val campos = call.receive<Map<String, String>>()
        val providerId = campos["provider_id"]?.toIntOrNull()
        val serviceType = campos["service_type"]
        val basePrice = campos["base_price"]?.toDoubleOrNull() ?: 0.0
        val pricePerKm = campos["price_per_km"]?.toDoubleOrNull() ?: 0.0

        if (id == null || providerId == null || serviceType.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Dados inválidos"))
            return@put
        }

        val sucesso = atualizarDadosServicoNoBanco(id, providerId, serviceType, basePrice, pricePerKm)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Serviço atualizado!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao atualizar"))
        }
    }

    // PATCH: Ativa ou desativa o serviço instantaneamente via Switch
    patch("/alternar-status-servico/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val campos = call.receive<Map<String, String>>()
        val providerId = campos["provider_id"]?.toIntOrNull()
        val isActive = campos["is_active"]?.toBooleanStrictOrNull()

        if (id == null || providerId == null || isActive == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Parâmetros de status inválidos"))
            return@patch
        }

        val sucesso = alternarStatusServicoNoBanco(id, providerId, isActive)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Status alterado com sucesso!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao mudar status"))
        }
    }

    // DELETE: Exclui o serviço do cardápio
    delete("/excluir-servico/{id}/{providerId}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val providerId = call.parameters["providerId"]?.toIntOrNull()

        if (id == null || providerId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "IDs inválidos"))
            return@delete
        }

        val sucesso = excluirServicoNoBanco(id, providerId)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Serviço removido!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao remover"))
        }
    }
}

