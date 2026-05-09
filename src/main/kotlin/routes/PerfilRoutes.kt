package com.example.routes

import com.example.database.buscarPerfilNoBanco
import com.example.database.atualizarPerfilNoBanco
import com.example.database.buscarServicosDaOficina
import com.example.models.AuthResponse
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

}

