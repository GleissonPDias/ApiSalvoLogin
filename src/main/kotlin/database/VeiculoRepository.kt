package com.example.database

import com.example.models.ProviderVehicleResponse

// 1. CRIAR (POST)
fun adicionarVeiculoNoBanco(providerId: Int, name: String, plate: String, status: String, photoBase64: String?): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "INSERT INTO provider_vehicles (provider_id, name, plate, status, vehicle_photo, is_active) VALUES (?, ?, ?, ?, ?, 1)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, providerId)
                stmt.setString(2, name)
                stmt.setString(3, plate)
                stmt.setString(4, status)
                stmt.setString(5, photoBase64)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro adicionarVeiculo: ${e.message}")
        false
    }
}

// 2. LER (GET)
fun buscarVeiculosDaOficina(providerId: Int): List<ProviderVehicleResponse> {
    val lista = mutableListOf<ProviderVehicleResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "SELECT id, provider_id, name, plate, status, vehicle_photo, is_active FROM provider_vehicles WHERE provider_id = ? AND is_active = 1 ORDER BY id DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, providerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        lista.add(
                            ProviderVehicleResponse(
                                id = rs.getInt("id"),
                                provider_id = rs.getInt("provider_id"),
                                name = rs.getString("name"),
                                plate = rs.getString("plate"),
                                status = rs.getString("status"),
                                vehicle_photo = rs.getString("vehicle_photo"),
                                is_active = rs.getBoolean("is_active")
                            )
                        )
                    }
                }
            }
            lista
        }
    } catch (e: Exception) {
        println("Erro buscarVeiculos: ${e.message}")
        emptyList()
    }
}

// 3. ATUALIZAR STATUS (PATCH)
fun atualizarStatusVeiculoNoBanco(id: Int, providerId: Int, status: String): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "UPDATE provider_vehicles SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND provider_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, status)
                stmt.setInt(2, id)
                stmt.setInt(3, providerId)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro atualizarStatusVeiculo: ${e.message}")
        false
    }
}

// 4. EXCLUIR / SOFT DELETE (DELETE)
fun excluirVeiculoNoBanco(id: Int, providerId: Int): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Usamos Soft Delete (is_active = 0) para não quebrar históricos de resgates passados
            val sql = "UPDATE provider_vehicles SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND provider_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.setInt(2, providerId)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro excluirVeiculo: ${e.message}")
        false
    }
}

fun atualizarDadosVeiculoNoBanco(id: Int, providerId: Int, name: String, plate: String, photoBase64: String?): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Se veio foto nova, atualiza tudo. Se não veio, atualiza só nome e placa.
            val sql = if (!photoBase64.isNullOrBlank()) {
                "UPDATE provider_vehicles SET name = ?, plate = ?, vehicle_photo = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND provider_id = ?"
            } else {
                "UPDATE provider_vehicles SET name = ?, plate = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND provider_id = ?"
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, name)
                stmt.setString(2, plate)

                if (!photoBase64.isNullOrBlank()) {
                    stmt.setString(3, photoBase64)
                    stmt.setInt(4, id)
                    stmt.setInt(5, providerId)
                } else {
                    stmt.setInt(3, id)
                    stmt.setInt(4, providerId)
                }
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro atualizarDadosVeiculo: ${e.message}")
        false
    }
}