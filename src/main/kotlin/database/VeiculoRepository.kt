package com.example.database

import com.example.models.ProviderVehicleResponse
import com.example.models.VeiculoRequest

// 1. CRIAR (POST) - Usando a Data Class 'VeiculoRequest'
fun adicionarVeiculoNoBanco(providerId: Int, veiculo: VeiculoRequest): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = """
                INSERT INTO provider_vehicles 
                (provider_id, name, plate, status, vehicle_photo, brand, vehicle_type, maintenance_date, is_active) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, providerId)
                stmt.setString(2, veiculo.name)
                stmt.setString(3, veiculo.plate)
                stmt.setString(4, veiculo.status ?: "Disponível")
                stmt.setString(5, veiculo.vehicle_photo)
                stmt.setString(6, veiculo.brand)
                stmt.setString(7, veiculo.vehicle_type)
                stmt.setString(8, veiculo.maintenance_date)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro adicionarVeiculo: ${e.message}")
        false
    }
}

// 2. LER (GET) - Empacotando e retornando a 'ProviderVehicleResponse'
fun buscarVeiculosDaOficina(providerId: Int): List<ProviderVehicleResponse> {
    val lista = mutableListOf<ProviderVehicleResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = """
                SELECT id, provider_id, name, brand, vehicle_type, maintenance_date, plate, status, vehicle_photo, is_active 
                FROM provider_vehicles 
                WHERE provider_id = ? AND is_active = 1 
                ORDER BY id DESC
            """.trimIndent()

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
                                is_active = rs.getBoolean("is_active"),
                                // Novos campos:
                                brand = rs.getString("brand"),
                                vehicle_type = rs.getString("vehicle_type"),
                                maintenance_date = rs.getString("maintenance_date")
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

// 3. ATUALIZAR STATUS RÁPIDO (PATCH) - Para ativar/desativar o veículo rapidamente
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

// 4. EXCLUIR / SOFT DELETE (DELETE) - Apenas esconde da lista (is_active = 0)
fun excluirVeiculoNoBanco(id: Int, providerId: Int): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
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

// 5. ATUALIZAR DADOS COMPLETOS DO VEÍCULO (PUT) - Usando a Data Class 'VeiculoRequest'
fun atualizarDadosVeiculoNoBanco(providerId: Int, veiculo: VeiculoRequest): Boolean {
    // 🛡️ Trava de segurança: Para atualizar, o ID do veículo não pode ser nulo!
    if (veiculo.id == null) return false

    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Se veio uma foto nova, atualizamos ela. Se não, mantemos a que já estava.
            val sql = if (!veiculo.vehicle_photo.isNullOrBlank()) {
                """
                UPDATE provider_vehicles 
                SET name = ?, plate = ?, brand = ?, vehicle_type = ?, maintenance_date = ?, vehicle_photo = ?, updated_at = CURRENT_TIMESTAMP 
                WHERE id = ? AND provider_id = ?
                """
            } else {
                """
                UPDATE provider_vehicles 
                SET name = ?, plate = ?, brand = ?, vehicle_type = ?, maintenance_date = ?, updated_at = CURRENT_TIMESTAMP 
                WHERE id = ? AND provider_id = ?
                """
            }

            conn.prepareStatement(sql.trimIndent()).use { stmt ->
                stmt.setString(1, veiculo.name)
                stmt.setString(2, veiculo.plate)
                stmt.setString(3, veiculo.brand)
                stmt.setString(4, veiculo.vehicle_type)
                stmt.setString(5, veiculo.maintenance_date)

                if (!veiculo.vehicle_photo.isNullOrBlank()) {
                    stmt.setString(6, veiculo.vehicle_photo)
                    stmt.setInt(7, veiculo.id)
                    stmt.setInt(8, providerId)
                } else {
                    stmt.setInt(6, veiculo.id)
                    stmt.setInt(7, providerId)
                }
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro atualizarDadosVeiculo: ${e.message}")
        false
    }
}

// ================================================================
// REPOSITÓRIO: VEÍCULOS DOS CLIENTES (customer_vehicles)
// ================================================================

// 1. CRIAR VEÍCULO DO CLIENTE (Corrigido com colunas obrigatórias)
fun adicionarVeiculoClienteNoBanco(customerId: Int, modelo: String, placa: String, marca: String, cor: String, tipo: String) {
    DatabaseConfig.getConnection().use { conn ->
        val sql = """
            INSERT INTO customer_vehicles 
            (customer_id, model, plate, is_active, vehicle_type, brand, color) 
            VALUES (?, ?, ?, 1, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, customerId)
            stmt.setString(2, modelo)
            stmt.setString(3, placa)
            stmt.setString(4, tipo)   // 🚀 Injeta o valor real
            stmt.setString(5, marca)  // 🚀 Injeta o valor real
            stmt.setString(6, cor)    // 🚀 Injeta o valor real
            stmt.executeUpdate()
        }
    }
}

// 2. BUSCAR VEÍCULOS DO CLIENTE
fun buscarVeiculosDoCliente(customerId: Int): List<ProviderVehicleResponse> {
    val lista = mutableListOf<ProviderVehicleResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Trocado 'name' por 'model' e removido 'status' e 'vehicle_photo'
            val sql = """
                SELECT id, customer_id, model, plate, is_active 
                FROM customer_vehicles 
                WHERE customer_id = ? AND is_active = 1 
                ORDER BY id DESC
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, customerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        lista.add(
                            ProviderVehicleResponse(
                                id = rs.getInt("id"),
                                provider_id = rs.getInt("customer_id"),
                                name = rs.getString("model"), // Mapeia a coluna 'model' pro campo 'name' do Kotlin
                                plate = rs.getString("plate"),
                                status = "Ativo", // Força um status genérico já que não tem na tabela
                                vehicle_photo = "", // Retorna vazio já que não tem na tabela
                                is_active = rs.getBoolean("is_active")
                            )
                        )
                    }
                }
            }
            lista
        }
    } catch (e: Exception) {
        println("Erro buscarVeiculosDoCliente: ${e.message}")
        emptyList()
    }
}

// 3. EXCLUIR VEÍCULO DO CLIENTE (Soft Delete)
fun excluirVeiculoClienteNoBanco(id: Int, customerId: Int): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "UPDATE customer_vehicles SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND customer_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.setInt(2, customerId)
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro excluirVeiculoCliente: ${e.message}")
        false
    }
}

// 4. ATUALIZAR DADOS VEÍCULO DO CLIENTE
fun atualizarDadosVeiculoClienteNoBanco(id: Int, customerId: Int, nome: String, placa: String, foto: String?): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Atualizamos para usar 'model' em vez de 'name', e removemos a 'vehicle_photo'
            val sql = "UPDATE customer_vehicles SET model = ?, plate = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND customer_id = ?"
            
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nome) // O Kotlin chama de 'nome', mas o SQL salva em 'model'
                stmt.setString(2, placa)
                stmt.setInt(3, id)
                stmt.setInt(4, customerId)
                
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro atualizarDadosVeiculoCliente: ${e.message}")
        false
    }
}

// ================================================================
// REPOSITÓRIO: AVALIAÇÃO DE PEDIDOS
// ================================================================
fun salvarAvaliacaoNoBanco(pedidoId: Int, nota: Int, comentario: String): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // 🚀 TRUQUE MÁGICO DO SQL: 
            // Inserimos na service_reviews copiando o cliente e o prestador direto da service_requests!
            val sql = """
                INSERT INTO service_reviews (request_id, customer_id, provider_id, rating, comment)
                SELECT id, customer_id, assigned_provider_id, ?, ?
                FROM service_requests
                WHERE id = ?
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, nota)           // Primeiro '?' -> rating
                stmt.setString(2, comentario)  // Segundo '?' -> comment
                stmt.setInt(3, pedidoId)       // Terceiro '?' -> WHERE id = ?
                
                stmt.executeUpdate() > 0
            }
        }
    } catch (e: Exception) {
        println("Erro salvarAvaliacao: ${e.message}")
        false
    }
}

// 6. BUSCAR DADOS DE VEÍCULO DO CLIENTE FORMATADOS (Usado no Radar WebSocket)
fun obterDadosVeiculoCliente(vehicleId: Int): String {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "SELECT brand, model, plate FROM customer_vehicles WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, vehicleId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val brand = rs.getString("brand") ?: ""
                        val model = rs.getString("model") ?: ""
                        val plate = rs.getString("plate") ?: ""
                        if (brand.isNotEmpty() && model.isNotEmpty()) {
                            "$brand $model - $plate"
                        } else if (model.isNotEmpty()) {
                            "$model - $plate"
                        } else {
                            "Placa: $plate"
                        }
                    } else {
                        "Veículo não identificado"
                    }
                }
            }
        }
    } catch (e: Exception) {
        "Veículo não identificado"
    }
}