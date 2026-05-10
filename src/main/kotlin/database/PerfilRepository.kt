package com.example.database

import com.example.models.ProviderServiceResponse

fun atualizarPerfilNoBanco(userId: Int, campos: Map<String, String>): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // 1. Identifica o Role
            val sqlRole = "SELECT user_role FROM users WHERE user_id = ?"
            val stmtRole = conn.prepareStatement(sqlRole)
            stmtRole.setInt(1, userId)
            val rs = stmtRole.executeQuery()

            if (!rs.next()) return false
            val role = rs.getString("user_role").lowercase()

            // 2. Colunas permitidas (AJUSTADO PARA OS ROLES REAIS)
            val colunasPermitidas = when (role) {
                "provider", "oficina" -> listOf(
                    "user_name", "user_cpf_cnpj", "user_address",
                    "user_banner", "user_phone", "latitude", "longitude"
                )
                "customer", "cliente" -> listOf("user_name", "user_phone")
                else -> emptyList()
            }

            // 3. Verifica permissão
            val campoParaAtualizar = campos.keys.firstOrNull { it in colunasPermitidas }

            if (campoParaAtualizar == null) {
                println("Campo não permitido ou não autorizado para o role: $role")
                return false
            }

            // 4. Executa o Update
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

fun buscarPerfilNoBanco(id: Int): Map<String, Any?>? {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Query poderosa que busca os dados do usuário e já calcula a média de notas da tabela service_reviews
            val sql = """
                SELECT 
                    u.user_name, 
                    u.user_cpf_cnpj, 
                    u.user_address, 
                    u.user_banner,
                    COALESCE(AVG(sr.rating), 5.0) AS media_notas,
                    COUNT(sr.id) AS total_reviews
                FROM users u
                LEFT JOIN service_reviews sr ON u.user_id = sr.provider_id
                WHERE u.user_id = ?
                GROUP BY u.user_id
            """.trimIndent()

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                // Formata a média para ter apenas 1 casa decimal (ex: 4.8)
                val mediaFormatada = String.format("%.1f", rs.getDouble("media_notas"))

                mapOf(
                    "nome" to rs.getString("user_name"),
                    "cnpj" to rs.getString("user_cpf_cnpj"),
                    "endereco" to rs.getString("user_address"),
                    "banner" to rs.getString("user_banner"),
                    "rating" to mediaFormatada,
                    "reviews" to rs.getInt("total_reviews")
                )
            } else null
        }
    } catch (e: Exception) {
        println("Erro buscarPerfil: ${e.message}")
        null
    }
}

