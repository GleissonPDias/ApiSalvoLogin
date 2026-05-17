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

            // 2. Colunas permitidas (Mantido igual ao seu original)
            val colunasPermitidas = when (role) {
                "provider", "oficina" -> listOf(
                    "user_name", "user_cpf_cnpj", "user_address",
                    "user_banner", "user_phone", "latitude", "longitude"
                )
                "customer", "cliente" -> listOf("user_name", "user_phone")
                else -> emptyList()
            }

            // 3. Filtra TODOS os campos permitidos que vieram na requisição
            val camposValidos = campos.filterKeys { it in colunasPermitidas }

            if (camposValidos.isEmpty()) {
                println("Nenhum campo válido ou autorizado para atualizar no role: $role")
                return false
            }

            // 4. Monta a Query Dinâmica (Ex: "user_address = ?, latitude = ?, longitude = ?")
            val colunasSql = camposValidos.keys.joinToString(", ") { "$it = ?" }
            val sqlUpdate = "UPDATE users SET $colunasSql, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?"

            val statement = conn.prepareStatement(sqlUpdate)

            // 5. Preenche os '?' com os valores correspondentes
            var index = 1
            for (valor in camposValidos.values) {
                statement.setString(index, valor)
                index++
            }
            // O último '?' é sempre o ID do usuário
            statement.setInt(index, userId)

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

fun atualizarStatusOnline(providerId: Int, isOnline: Boolean): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "UPDATE provider_profiles SET is_receiving_requests = ? WHERE provider_id = ?"
            val stmt = conn.prepareStatement(sql)

            // No MySQL, TRUE é 1 e FALSE é 0
            stmt.setInt(1, if (isOnline) 1 else 0)
            stmt.setInt(2, providerId)

            val linhasAfetadas = stmt.executeUpdate()
            linhasAfetadas > 0 // Retorna true se atualizou com sucesso
        }
    } catch (e: Exception) {
        println("Erro ao atualizar status online do prestador: ${e.message}")
        false
    }
}

