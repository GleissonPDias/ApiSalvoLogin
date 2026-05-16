package com.example.database

import com.example.models.PedidoPendenteResponse
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

fun buscarPedidosDoPrestador(providerId: Int): List<PedidoPendenteResponse> {
    val listaPedidos = mutableListOf<PedidoPendenteResponse>()

    return try {
        DatabaseConfig.getConnection().use { conn ->
            // Aqui está a mágica do JOIN puxando cliente e veículo de uma vez só!
            val sql = """
                SELECT 
                    sm.id AS match_id,
                    sr.id AS request_id,
                    sr.service_type,
                    sr.description,
                    sr.location_lat,
                    sr.location_lng,
                    c.user_name AS nome_cliente,
                    c.user_phone AS telefone_cliente,
                    cv.brand AS marca_veiculo,
                    cv.model AS modelo_veiculo,
                    cv.plate AS placa_veiculo,
                    cv.year AS ano_veiculo
                FROM service_matches sm
                JOIN service_requests sr ON sm.request_id = sr.id
                JOIN users c ON sr.customer_id = c.user_id
                JOIN customer_vehicles cv ON sr.vehicle_id = cv.id
                WHERE sm.provider_id = ? AND sm.status = 'pending'
            """.trimIndent()

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, providerId) // Filtra apenas os pedidos 'pending' deste mecânico

            val rs = stmt.executeQuery()

            while (rs.next()) {
                listaPedidos.add(
                    PedidoPendenteResponse(
                        matchId = rs.getInt("match_id"),
                        requestId = rs.getInt("request_id"),
                        serviceType = rs.getString("service_type"),
                        description = rs.getString("description"),
                        latitude = rs.getDouble("location_lat"),
                        longitude = rs.getDouble("location_lng"),
                        clienteNome = rs.getString("nome_cliente"),
                        clienteTelefone = rs.getString("telefone_cliente") ?: "",
                        veiculoMarca = rs.getString("marca_veiculo"),
                        veiculoModelo = rs.getString("modelo_veiculo"),
                        veiculoPlaca = rs.getString("placa_veiculo"),
                        veiculoAno = rs.getString("ano_veiculo") ?: ""
                    )
                )
            }
        }
        listaPedidos
    } catch (e: Exception) {
        println("Erro ao buscar pedidos do prestador: ${e.message}")
        emptyList() // Retorna lista vazia em caso de erro
    }

}