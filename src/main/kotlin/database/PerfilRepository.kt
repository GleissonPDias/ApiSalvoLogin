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
            // Buscando os dados que o Android precisa mostrar na tela
            val sql = "SELECT user_name, user_cpf_cnpj, user_address, user_banner FROM users WHERE user_id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                mapOf(
                    "nome" to rs.getString("user_name"),
                    "cnpj" to rs.getString("user_cpf_cnpj"),
                    "endereco" to rs.getString("user_address"),
                    "banner" to rs.getString("user_banner")
                )
            } else null
        }
    } catch (e: Exception) {
        println("Erro buscarPerfil: ${e.message}")
        null
    }
}

fun buscarServicosDaOficina(providerId: Int): List<ProviderServiceResponse> {
    val lista = mutableListOf<ProviderServiceResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "SELECT id, service_type, base_price, price_per_km, is_active FROM provider_services WHERE provider_id = ? AND is_active = 1"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, providerId)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                lista.add(
                    ProviderServiceResponse(
                        id = rs.getInt("id"),
                        service_type = rs.getString("service_type"),
                        base_price = rs.getDouble("base_price"),
                        price_per_km = rs.getDouble("price_per_km"),
                        is_active = rs.getBoolean("is_active")
                    )
                )
            }
            lista
        }
    } catch (e: Exception) {
        println("Erro buscarServicos: ${e.message}")
        emptyList()
    }
}