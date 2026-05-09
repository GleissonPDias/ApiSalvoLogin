package com.example.database

import com.example.models.PedidosResponse

fun buscarPedidos(userId: Int): List<PedidosResponse> {
    val listaPedidos = mutableListOf<PedidosResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Query buscando da tabela nova e fazendo JOIN para pegar o nome do prestador
            val sql = """
                SELECT 
                    sr.id, 
                    sr.customer_id, 
                    sr.service_type, 
                    sr.description, 
                    sr.vehicle_info, 
                    sr.status, 
                    sr.assigned_provider_id, 
                    u.user_name AS prestador_nome,
                    sr.final_price, 
                    sr.final_distance, 
                    sr.destino_address, 
                    sr.created_at
                FROM service_requests sr
                LEFT JOIN users u ON sr.assigned_provider_id = u.user_id
                WHERE sr.customer_id = ?
                ORDER BY sr.created_at DESC
            """.trimIndent()

            val statement = conn.prepareStatement(sql)
            statement.setInt(1, userId)
            val rs = statement.executeQuery()

            while (rs.next()) {
                // Leitura segura de inteiros que podem ser nulos
                val providerId = rs.getInt("assigned_provider_id")
                val assignedProviderIdSafe = if (rs.wasNull()) null else providerId

                // Leitura segura de decimais/doubles que podem ser nulos
                val price = rs.getDouble("final_price")
                val finalPriceSafe = if (rs.wasNull()) null else price

                val distance = rs.getDouble("final_distance")
                val finalDistanceSafe = if (rs.wasNull()) null else distance

                listaPedidos.add(
                    PedidosResponse(
                        id = rs.getInt("id"),
                        customer_id = rs.getInt("customer_id"),
                        service_type = rs.getString("service_type"),
                        description = rs.getString("description"),
                        vehicle_info = rs.getString("vehicle_info"),
                        status = rs.getString("status"),
                        assigned_provider_id = assignedProviderIdSafe,
                        prestador_nome = rs.getString("prestador_nome"), // Vem do JOIN
                        final_price = finalPriceSafe,
                        final_distance = finalDistanceSafe,
                        destino_address = rs.getString("destino_address"),
                        created_at = rs.getString("created_at") ?: ""
                    )
                )
            }
            listaPedidos
        }
    } catch (e: Exception) {
        println("Erro ao buscar pedidos: ${e.message}")
        emptyList()
    }
}