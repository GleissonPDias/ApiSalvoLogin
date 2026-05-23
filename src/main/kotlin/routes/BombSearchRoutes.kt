package com.example.routes

import com.example.database.BombSearchRepository
import com.example.models.PartidaBombSearchRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.bombSearchRoutes(repository: BombSearchRepository) {
    route("/bombsearch") {

        // ROTA EXISTENTE: Salvar partida (POST)
        post("/salvar_partida") {
            try {
                val partida = call.receive<PartidaBombSearchRequest>()
                val sucesso = repository.salvarPartida(partida)

                if (sucesso) {
                    call.respond(HttpStatusCode.Created, mapOf("mensagem" to "Partida salva com sucesso!"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to "Falha ao salvar a partida"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("erro" to "Dados inválidos"))
            }
        }

        // NOVA ROTA: Buscar Histórico (GET)
        get("/historico") {
            try {
                // Chama a função do repositório que acabamos de criar
                val historico = repository.buscarHistorico()

                // Devolve a lista de partidas (o Gson vai transformar isso em JSON automaticamente)
                call.respond(HttpStatusCode.OK, historico)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to "Falha ao buscar histórico"))
            }
        }
    }
}