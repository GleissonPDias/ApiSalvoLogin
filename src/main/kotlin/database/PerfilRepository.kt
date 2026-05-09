package com.example.database

fun atualizarPerfilNoBanco(userId: Int, campos: Map<String, String>): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // 1. Primeiro, descobrimos quem é esse usuário (Role Check)
            val sqlRole = "SELECT user_role FROM users WHERE user_id = ?"
            val stmtRole = conn.prepareStatement(sqlRole)
            stmtRole.setInt(1, userId)
            val rs = stmtRole.executeQuery()

            if (!rs.next()) return false
            val role = rs.getString("user_role")

            // 2. Definimos o que cada um pode editar
            val colunasPermitidas = when (role.lowercase()) {
                "oficina" -> listOf("user_name", "user_cpf_cnpj", "user_address", "user_banner", "user_phone")
                "cliente" -> listOf("user_name", "user_phone") // Cliente não edita banner nem CNPJ (usaria CPF)
                else -> emptyList()
            }

            // 3. Verificamos se o campo enviado está na lista permitida para aquele Role
            val campoParaAtualizar = campos.keys.firstOrNull { it in colunasPermitidas }

            if (campoParaAtualizar == null) {
                println("Tentativa de edição não autorizada para o role: $role")
                return false
            }

            // 4. Executamos o Update
            val sqlUpdate = "UPDATE users SET $campoParaAtualizar = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?"
            val statement = conn.prepareStatement(sqlUpdate)

            statement.setString(1, campos[campoParaAtualizar])
            statement.setInt(2, userId)

            statement.executeUpdate() > 0
        }
    } catch (e: Exception) {
        println("Erro PerfilRepository: ${e.message}")
        false
    }
}