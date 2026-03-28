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
import java.sql.DriverManager // Importante para o banco

// Modelos (Devem bater com o que o Android envia/recebe)
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
                val pedido = call.receive<LoginRequest>()

                // Chamamos a função que valida no banco de dados real
                val usuarioExiste = validarNoBanco(pedido.email, pedido.password)

                if (usuarioExiste) {
                    call.respond(AuthResponse(true, "Login realizado com sucesso!"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, AuthResponse(false, "E-mail ou senha incorretos"))
                }
            }
        }
    }.start(wait = true)
}

// FUNÇÃO DE CONEXÃO (Ajuste os dados conforme seu banco online)
fun validarNoBanco(email: String, senha: String): Boolean {
    Class.forName("com.mysql.cj.jdbc.Driver")
    // Exemplo para MySQL (Se for outro banco, a URL muda)
    val url = "jdbc:mysql://www.thyagoquintas.com.br:3306/engenharia_339"
    val user = "engenharia_339"
    val password = "capivara"

    return try {
        // 1. Conecta ao banco
        DriverManager.getConnection(url, user, password).use { conn ->
            // 2. Prepara a Query (Protege contra SQL Injection)
            val sql = "SELECT * FROM USUARIO WHERE USUARIO_EMAIL = ? AND USUARIO_SENHA = ?"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, email)
            statement.setString(2, senha)

            // 3. Executa e vê se retornou algum registro
            val resultSet = statement.executeQuery()
            resultSet.next() // Retorna true se houver uma linha correspondente
        }
    } catch (e: Exception) {
        println("Erro no banco: ${e.message}")
        false
    }
}