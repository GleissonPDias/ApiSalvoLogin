package com.example.routes

import com.example.database.*
import com.example.models.VeiculoRequest
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
        try {
            // 🔥 Ktor converte o JSON automaticamente para VeiculoRequest
            val veiculo = call.receive<VeiculoRequest>()
            val providerId = veiculo.provider_id

            if (providerId == null || veiculo.name.isBlank() || veiculo.plate.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Campos obrigatórios ausentes"))
                return@post
            }

            // Chama o repositório enviando os 2 argumentos
            val sucesso = adicionarVeiculoNoBanco(providerId, veiculo)

            if (sucesso) {
                call.respond(HttpStatusCode.Created, mapOf("sucesso" to true, "mensagem" to "Veículo cadastrado com sucesso!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao salvar veículo no banco"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Erro: ${e.message}"))
        }
    }

    // ATUALIZAR STATUS (Rápido): Muda o status (Ex: de 'Disponível' para 'Em atendimento')
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

    // EXCLUIR: Remove o veículo da lista (Soft Delete)
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

    // ATUALIZAR DADOS COMPLETOS: Edita nome, placa, marca, foto, etc.
    put("/atualizar-veiculo/{id}") {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
            val veiculo = call.receive<VeiculoRequest>()

            // Verifica se o ID e o providerId existem
            if (id == null || veiculo.provider_id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "ID do veículo ou da oficina ausentes"))
                return@put
            }

            // 🔥 Injeta o ID da URL no objeto do veículo para não ter erro na hora de salvar
            val veiculoAtualizado = veiculo.copy(id = id)

            // Chama o repositório enviando os 2 argumentos corretos!
            val sucesso = atualizarDadosVeiculoNoBanco(veiculoAtualizado.provider_id!!, veiculoAtualizado)

            if (sucesso) {
                call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Veículo atualizado!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao atualizar"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Erro na formatação: ${e.message}"))
        }
    }
// ================================================================
    // ROTAS: VEÍCULOS DOS CLIENTES (customer_vehicles)
    // ================================================================

    // LER: Retorna os veículos do cliente
    get("/veiculos-cliente/{customerId}") {
        val customerId = call.parameters["customerId"]?.toIntOrNull()
        if (customerId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "ID do cliente inválido"))
            return@get
        }
        val veiculos = buscarVeiculosDoCliente(customerId)
        call.respond(HttpStatusCode.OK, veiculos)
    }

    // CRIAR: Cadastra um novo carro pro cliente
    post("/adicionar-veiculo-cliente") {
        try {
            val dados = call.receive<Map<String, String?>>()
            val customerId = dados["customer_id"]?.toIntOrNull()
            val nome = dados["name"]
            val placa = dados["plate"]
            val marca = dados["brand"] ?: "Não informada"        // 🚀 Pega do Android
            val cor = dados["color"] ?: "Não informada"          // 🚀 Pega do Android
            val tipo = dados["vehicle_type"] ?: "Carro"          // 🚀 Pega do Android
            val foto = dados["vehicle_photo"]

            if (customerId == null || nome.isNullOrBlank() || placa.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Campos obrigatórios ausentes"))
                return@post
            }

            // Repassa os novos valores para a função do banco
            adicionarVeiculoClienteNoBanco(customerId, nome, placa, marca, cor, tipo, foto)

            call.respond(HttpStatusCode.Created, mapOf("sucesso" to true, "mensagem" to "Veículo do cliente cadastrado!"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro: ${e.message}"))
        }
    }

    // ATUALIZAR: Edita o carro do cliente
    put("/atualizar-veiculo-cliente/{id}") {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
            val dados = call.receive<Map<String, String?>>()
            val customerId = dados["customer_id"]?.toIntOrNull()
            val nome = dados["name"]
            val placa = dados["plate"]
            val brand = dados["brand"]
            val color = dados["color"]
            val vehicleType = dados["vehicle_type"]
            val foto = dados["vehicle_photo"]

            if (id == null || customerId == null || nome.isNullOrBlank() || placa.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Campos inválidos para atualização"))
                return@put
            }

            val sucesso = atualizarDadosVeiculoClienteNoBanco(id, customerId, nome, placa, brand, color, vehicleType, foto)

            if (sucesso) {
                call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Veículo do cliente atualizado!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao atualizar no banco"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Erro: ${e.message}"))
        }
    }

    // EXCLUIR: Remove o carro da lista do cliente
    delete("/excluir-veiculo-cliente/{id}/{customerId}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val customerId = call.parameters["customerId"]?.toIntOrNull()

        if (id == null || customerId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "IDs inválidos"))
            return@delete
        }

        val sucesso = excluirVeiculoClienteNoBanco(id, customerId)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Veículo do cliente excluído!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao excluir veículo"))
        }
    }

    // ================================================================
    // AVALIAÇÃO DO SERVIÇO
    // ================================================================
    post("/avaliar-pedido") {
        try {
            val dados = call.receive<Map<String, String>>()
            val pedidoId = dados["pedidoId"]?.toIntOrNull()
            val nota = dados["nota"]?.toIntOrNull()
            val comentario = dados["comentario"] ?: ""

            if (pedidoId == null || nota == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "mensagem" to "Dados incompletos"))
                return@post
            }

            val sucesso = salvarAvaliacaoNoBanco(pedidoId, nota, comentario)

            if (sucesso) {
                call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "mensagem" to "Avaliação salva com sucesso!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to "Erro ao salvar avaliação"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "mensagem" to e.message))
        }
    }
}