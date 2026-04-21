package com.example.database

import com.example.models.AuthResponse
import com.example.models.RegisterRequest
import org.mindrot.jbcrypt.BCrypt


fun validarNoBanco(email: String, senhaDigitada: String): AuthResponse {
    return try{
        DatabaseConfig.getConnection().use { conn ->
            val sql = "SELECT user_password FROM users WHERE user_email = ?"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, email)

            val resultSet = statement.executeQuery()

            if(resultSet.next()) {
                val hashNoBanco = resultSet.getString("user_password")
                if(BCrypt.checkpw(senhaDigitada, hashNoBanco)){
                    AuthResponse(true, "Login realizado com sucesso!")
                }else{
                    AuthResponse(false, "E-mail ou senha incorretos")
                }
            } else{
                AuthResponse(false, "E-mail ou senha incorretos")
            }
        }
    } catch (e: Exception) {
        AuthResponse(false, "Erro Supabase: ${e.message}")
    }
}

fun cadastrarNoBanco(usuario: RegisterRequest): AuthResponse {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val senhaHasheada = BCrypt.hashpw(usuario.password, BCrypt.gensalt())

            val roleMapeado = when (usuario.role.lowercase().trim()) {
                "prestador", "provider", "oficina" -> "provider"
                "cliente", "customer" -> "customer"
                else -> "customer"
            }

            val sql = "INSERT INTO users (user_name, user_email, user_password, user_cpf_cnpj, user_phone, user_role) VALUES (?, ?, ?, ?, ?, ?)"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, usuario.nome)
            statement.setString(2, usuario.email)
            statement.setString(3, senhaHasheada)
            statement.setString(4, usuario.cpf)
            statement.setString(5, usuario.telefone)
            statement.setString(6, roleMapeado)

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
