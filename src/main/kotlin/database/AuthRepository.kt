package com.example.database

import com.example.models.AuthResponse
import com.example.models.RegisterRequest
import org.mindrot.jbcrypt.BCrypt
import com.example.models.GenericResponse


fun validarNoBanco(email: String, senhaDigitada: String): AuthResponse {
    return try{
        DatabaseConfig.getConnection().use { conn ->
            val sql = "SELECT user_id, user_name, user_role, user_password FROM users WHERE user_email = ?"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, email)

            val resultSet = statement.executeQuery()

            if(resultSet.next()) {
                val hashNoBanco = resultSet.getString("user_password")
                if(BCrypt.checkpw(senhaDigitada, hashNoBanco)){

                    val idBanco = resultSet.getInt("user_id")
                    val nomeBanco = resultSet.getString("user_name")
                    val roleBanco = resultSet.getString("user_role")

                    AuthResponse(
                        sucesso = true,
                        message = "Login realizado com sucesso!",
                        userId = idBanco,
                        nome = nomeBanco,
                        role = roleBanco
                    )
                }else{
                    AuthResponse(false, "E-mail ou senha incorretos", null, null, null)
                }
            } else{
                AuthResponse(false, "E-mail ou senha incorretos", null, null, null)
            }
        }
    } catch (e: Exception) {
        AuthResponse(false, "Erro Supabase: ${e.message}", null, null, null)
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

            val sql = "INSERT INTO users (user_name, user_email, user_password, user_cpf_cnpj, user_phone, user_role, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?,?,?)"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, usuario.nome)
            statement.setString(2, usuario.email)
            statement.setString(3, senhaHasheada)
            statement.setString(4, usuario.cpf)
            statement.setString(5, usuario.telefone)
            statement.setString(6, roleMapeado)
            statement.setDouble(7, usuario.latitude ?: 0.0)
            statement.setDouble(8, usuario.longitude ?: 0.0)

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

// NOVO: Função para verificar e solicitar a recuperação
fun solicitarRecuperacao(email: String): GenericResponse {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Busca apenas o ID para ver se o e-mail existe
            val sql = "SELECT user_id FROM users WHERE user_email = ?"
            val statement = conn.prepareStatement(sql)
            statement.setString(1, email)

            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                // O e-mail EXISTE no banco!
                // ATENÇÃO: Aqui no futuro você colocaria o código para disparar um e-mail real
                // usando uma biblioteca como o JavaMail. Por enquanto, vamos retornar sucesso.
                GenericResponse(true, "E-mail de recuperação enviado!")
            } else {
                // O e-mail NÃO EXISTE no banco
                GenericResponse(false, "E-mail não encontrado ou inválido.")
            }
        }
    } catch (e: Exception) {
        GenericResponse(false, "Erro no banco: ${e.message}")
    }
}