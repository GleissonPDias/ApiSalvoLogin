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
import org.mindrot.jbcrypt.BCrypt
import java.sql.DriverManager

// ==========================================
// 1. ADICIONAMOS O CAMPO 'ROLE' NO PEDIDO
// ==========================================
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val nome: String,
    val email: String,
    val cpf: String,
    val password: String,
    val telefone: String = "Não informado",
    val role: String // O Android vai mandar "cliente" ou "prestador" aqui
)
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
                    println("==== TENTATIVA DE CADASTRO: ${pedido.email} como ${pedido.role} ====")

                    val resposta = cadastrarNoBanco(pedido)

                    if (resposta.sucesso) {
                        call.respond(HttpStatusCode.Created, resposta)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, resposta)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "Erro na API: ${e.message}"))
                }
            }
        }
    }.start(wait = true)
}

// ==========================================
// FUNÇÕES DE BANCO DE DADOS (SUPABASE)
// ==========================================

fun validarNoBanco(email: String, senhaDigitada: String): AuthResponse {
    return try {
        Class.forName("org.postgresql.Driver")
        val url = "jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require"
        val user = "postgres.rlifesgqxjgdhulthcnw"
        val password = "Senacsp@2026"

        DriverManager.getConnection(url, user, password).use { conn ->
            val sql = "SELECT user_password FROM users WHERE user_email = ?"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, email)

            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val hashNoBanco = resultSet.getString("user_password")
                if (BCrypt.checkpw(senhaDigitada, hashNoBanco)) {
                    AuthResponse(true, "Login realizado com sucesso!")
                } else {
                    AuthResponse(false, "E-mail ou senha incorretos")
                }
            } else {
                AuthResponse(false, "E-mail ou senha incorretos")
            }
        }
    } catch (e: Exception) {
        AuthResponse(false, "Erro Supabase: ${e.message}")
    }
}

fun cadastrarNoBanco(usuario: RegisterRequest): AuthResponse {
    return try {
        Class.forName("org.postgresql.Driver")
        val url = "jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require"
        val user = "postgres.rlifesgqxjgdhulthcnw"
        val password = "Senacsp@2026"

        DriverManager.getConnection(url, user, password).use { conn ->
            val senhaHasheada = BCrypt.hashpw(usuario.password, BCrypt.gensalt())

            // ==========================================
            // 2. LÓGICA DE TRADUÇÃO E SEGURANÇA DO ROLE
            // ==========================================
            val roleMapeado = when (usuario.role.lowercase().trim()) {
                "prestador", "provider", "oficina" -> "provider"
                "cliente", "customer" -> "customer"
                else -> "customer" // Se vier algo esquisito ou vazio, vira cliente por segurança
            }

            // 3. INSERINDO O ROLE NA QUERY
            val sql = "INSERT INTO users (user_name, user_email, user_password, user_cpf_cnpj, user_phone, user_role) VALUES (?, ?, ?, ?, ?, ?)"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, usuario.nome)
            statement.setString(2, usuario.email)
            statement.setString(3, senhaHasheada)
            statement.setString(4, usuario.cpf)
            statement.setString(5, usuario.telefone)
            statement.setString(6, roleMapeado) // Adicionando a 6ª variável

            val linhasAfetadas = statement.executeUpdate()

            if (linhasAfetadas > 0) {
                AuthResponse(true, "Usuário cadastrado com sucesso!")
            } else {
                AuthResponse(false, "Falha ao cadastrar usuário.")
            }
        }
    } catch (e: Exception) {
        AuthResponse(false, "Erro ao cadastrar: ${e.message}")
    }
}