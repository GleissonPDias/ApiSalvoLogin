package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import java.sql.DriverManager

// Modelos
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val sucesso: Boolean, val mensagem: String)

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            gson { setPrettyPrinting() }
        }

        routing {
            post("/login") {
                try {
                    val pedido = call.receive<LoginRequest>()

                    println("==== 1. REQUISIÇÃO RECEBIDA DO ANDROID ====")
                    println("Email recebido: '${pedido.email}'")
                    println("Senha recebida: '${pedido.password}'")

                    // Agora a função retorna o AuthResponse completo (com o erro real, se houver)
                    val resposta = validarNoBanco(pedido.email, pedido.password)

                    if (resposta.sucesso) {
                        call.respond(HttpStatusCode.OK, resposta)
                    } else {
                        // Enviamos o erro detalhado de volta pro celular
                        call.respond(HttpStatusCode.Unauthorized, resposta)
                    }
                } catch (e: Exception) {
                    println("Erro no Ktor: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "Erro na API: ${e.message}"))
                }
            }
        }
    }.start(wait = true)
}

// FUNÇÃO DE CONEXÃO MODIFICADA (Retorna o AuthResponse para podermos ver o erro)
fun validarNoBanco(email: String, senha: String): AuthResponse {
    println("==== 2. TENTANDO CONECTAR AO BANCO DE DADOS ====")

    return try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val url = "jdbc:mysql://www.thyagoquintas.com.br:3306/engenharia_339"
        val user = "engenharia_339"
        val password = "capivara"

        DriverManager.getConnection(url, user, password).use { conn ->
            println("==== 3. CONEXÃO BEM-SUCEDIDA! ====")

            val sql = "SELECT * FROM USUARIO WHERE USUARIO_EMAIL = ? AND USUARIO_SENHA = ?"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, email)
            statement.setString(2, senha)

            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                println("==== 4. USUÁRIO ENCONTRADO! ====")
                AuthResponse(true, "Login realizado com sucesso!")
            } else {
                println("==== 4. USUÁRIO NÃO ENCONTRADO (Dados não batem) ====")
                AuthResponse(false, "E-mail ou senha incorretos")
            }
        }
    } catch (e: Exception) {
        println("==== ERRO FATAL NO BANCO ====")
        e.printStackTrace() // Imprime o erro completo no log do Render

        // A MÁGICA: Retorna o erro do MySQL direto para o Android!
        AuthResponse(false, "Erro MySQL: ${e.message}")
    }
}