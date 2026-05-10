package com.example.database

import com.example.models.ProviderServiceResponse

// 1. CRIAR (POST)
fun adicionarServicoNoBanco(providerId: Int, serviceType: String, basePrice: Double, pricePerKm: Double): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "INSERT INTO provider_services (provider_id, service_type, base_price, price_per_km, is_active) VALUES (?, ?, ?, ?, 1)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, providerId)
                stmt.setString(2, serviceType)
                stmt.setDouble(3, basePrice)
                stmt.setDouble(4, pricePerKm)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro adicionarServico: ${e.message}")
        false
    }
}

// 2. LER (GET)
fun buscarServicosDaOficina(providerId: Int): List<ProviderServiceResponse> {
    val lista = mutableListOf<ProviderServiceResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Traz todos os serviços (ativos e inativos) para o painel de gestão da oficina
            val sql = "SELECT id, provider_id, service_type, base_price, price_per_km, is_active FROM provider_services WHERE provider_id = ? ORDER BY id DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, providerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        lista.add(
                            ProviderServiceResponse(
                                id = rs.getInt("id"),
                                provider_id = rs.getInt("provider_id"),
                                service_type = rs.getString("service_type"),
                                base_price = rs.getDouble("base_price"),
                                price_per_km = rs.getDouble("price_per_km"),
                                is_active = rs.getString("is_active") == "1" || rs.getString("is_active") == "true"
                            )
                        )
                    }
                }
            }
            lista
        }
    } catch (e: Exception) {
        println("Erro buscarServicos: ${e.message}")
        emptyList()
    }
}

// 3. ATUALIZAR DADOS (PUT)
fun atualizarDadosServicoNoBanco(id: Int, providerId: Int, serviceType: String, basePrice: Double, pricePerKm: Double): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "UPDATE provider_services SET service_type = ?, base_price = ?, price_per_km = ? WHERE id = ? AND provider_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, serviceType)
                stmt.setDouble(2, basePrice)
                stmt.setDouble(3, pricePerKm)
                stmt.setInt(4, id)
                stmt.setInt(5, providerId)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro atualizarDadosServico: ${e.message}")
        false
    }
}

// 4. ATIVAR / DESATIVAR (PATCH) - Bloqueia ou libera o recebimento de pedidos
fun alternarStatusServicoNoBanco(id: Int, providerId: Int, isActive: Boolean): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "UPDATE provider_services SET is_active = ? WHERE id = ? AND provider_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, if (isActive) 1 else 0)
                stmt.setInt(2, id)
                stmt.setInt(3, providerId)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro alternarStatusServico: ${e.message}")
        false
    }
}

// 5. EXCLUIR DEFINITIVAMENTE (DELETE)
fun excluirServicoNoBanco(id: Int, providerId: Int): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "DELETE FROM provider_services WHERE id = ? AND provider_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.setInt(2, providerId)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro excluirServico: ${e.message}")
        false
    }
}