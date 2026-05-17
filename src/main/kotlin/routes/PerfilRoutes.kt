package com.example.routes

import com.example.database.buscarPerfilNoBanco
import com.example.database.atualizarPerfilNoBanco
import com.example.database.atualizarStatusOnline
import com.example.database.buscarServicosDaOficina
import com.example.models.GenericResponse // <-- MUDANÇA AQUI
import io.ktor.http.*
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.perfilRoutes() {

    // ROTA PARA BUSCAR DADOS (GET)
    get("/obter-perfil/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "ID inválido"))
            return@get
        }

        val dados = buscarPerfilNoBanco(id)
        if (dados != null) {
            call.respond(HttpStatusCode.OK, dados)
        } else {
            call.respond(HttpStatusCode.NotFound, GenericResponse(false, "Perfil não encontrado"))
        }
    }

    // ROTA PARA ATUALIZAR DADOS (PATCH)
    patch("/atualizar-perfil/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val campos = call.receive<Map<String, String>>()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, GenericResponse(false, "ID inválido"))
            return@patch
        }

        val sucesso = atualizarPerfilNoBanco(id, campos)
        if (sucesso) {
            call.respond(HttpStatusCode.OK, GenericResponse(true, "Alteração salva com sucesso!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, GenericResponse(false, "Erro ao atualizar banco"))
        }
    }

    get("/servicos-publicos/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to "ID inválido"))
            return@get
        }

        // 1. Busca a lista centralizada usando a função do ServicoRepository
        val todosServicos = buscarServicosDaOficina(id)

        // 2. Para a tela de perfil público, filtramos e devolvemos apenas os que estão Ativos (1)
        val servicosAtivos = todosServicos.filter { it.is_active }

        call.respond(HttpStatusCode.OK, servicosAtivos)
    }

    post("/atualizar-banner/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
        val multipart = call.receiveMultipart()
        var fileName = ""

        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                val fileBytes = part.streamProvider().readBytes()
                // Define o nome do arquivo (ex: banner_5.jpg)
                fileName = "banner_$id.jpg"

                // Salva o arquivo na pasta 'uploads' do seu servidor
                File("uploads/$fileName").writeBytes(fileBytes)
            }
            part.dispose()
        }

        // Agora salva o CAMINHO da imagem no banco de dados
        val urlImagem = "https://sua-api.com/uploads/$fileName"
        val sucesso = atualizarPerfilNoBanco(id, mapOf("user_banner" to urlImagem))

        if (sucesso) call.respond(HttpStatusCode.OK, "Banner atualizado!")
        else call.respond(HttpStatusCode.InternalServerError)
    }

    post("/provider/toggle-status") {
        try {
            // Recebe um mapa de strings enviadas pelo aplicativo
            val parametros = call.receive<Map<String, String>>()
            val providerId = parametros["provider_id"]?.toIntOrNull()
            val isOnline = parametros["is_online"]?.toBoolean() ?: false

            if (providerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("sucesso" to false, "message" to "ID inválido"))
                return@post
            }

            // Chama a função que criamos no passo anterior
            val sucesso = atualizarStatusOnline(providerId, isOnline)

            if (sucesso) {
                call.respond(HttpStatusCode.OK, mapOf("sucesso" to true, "isOnline" to isOnline))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "message" to "Erro ao atualizar banco"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("sucesso" to false, "message" to e.message))
        }
    }

}

