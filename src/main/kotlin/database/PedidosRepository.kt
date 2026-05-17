package com.example.database

import com.example.models.AceitarPedidoRequest
import com.example.models.OficinaDetalhesPolling
import com.example.models.PedidoPendenteResponse
import com.example.models.PedidosResponse
import com.example.models.PollingStatusResponse

fun buscarPedidos(userId: Int): List<PedidosResponse> {
    val listaPedidos = mutableListOf<PedidosResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = """
                SELECT 
                    sr.id, 
                    sr.customer_id, 
                    sr.service_type, 
                    sr.description, 
                    sr.vehicle_id, 
                    cv.brand,
                    cv.model,
                    cv.plate,
                    sr.status, 
                    sr.assigned_provider_id, 
                    u.user_name AS prestador_nome,
                    sr.final_price, 
                    sr.final_distance, 
                    sr.destino_address, 
                    sr.created_at
                FROM service_requests sr
                LEFT JOIN users u ON sr.assigned_provider_id = u.user_id
                LEFT JOIN customer_vehicles cv ON sr.vehicle_id = cv.id
                WHERE sr.customer_id = ?
                ORDER BY sr.created_at DESC
            """.trimIndent()

            val statement = conn.prepareStatement(sql)
            statement.setInt(1, userId)
            val rs = statement.executeQuery()

            while (rs.next()) {
                val providerId = rs.getInt("assigned_provider_id")
                val assignedProviderIdSafe = if (rs.wasNull()) null else providerId

                val price = rs.getDouble("final_price")
                val finalPriceSafe = if (rs.wasNull()) null else price

                val distance = rs.getDouble("final_distance")
                val finalDistanceSafe = if (rs.wasNull()) null else distance

                val brand = rs.getString("brand") ?: ""
                val model = rs.getString("model") ?: ""
                val plate = rs.getString("plate") ?: ""
                val vehicleInfoFormatted = if (brand.isNotEmpty() && model.isNotEmpty()) {
                    "$brand $model - $plate"
                } else {
                    "Veículo não informado"
                }

                listaPedidos.add(
                    PedidosResponse(
                        id = rs.getInt("id"),
                        customer_id = rs.getInt("customer_id"),
                        service_type = rs.getString("service_type"),
                        description = rs.getString("description"),
                        vehicle_info = vehicleInfoFormatted,
                        status = rs.getString("status"),
                        assigned_provider_id = assignedProviderIdSafe,
                        prestador_nome = rs.getString("prestador_nome"),
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
            stmt.setInt(1, providerId)

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
        emptyList()
    }
}

fun verificarStatusDoPedidoBanco(requestId: Int): PollingStatusResponse {
    var resposta = PollingStatusResponse(status = "unknown")

    try {
        DatabaseConfig.getConnection().use { conn ->
            // 1. Checa o status atual do pedido
            val sqlCheck = "SELECT status, assigned_provider_id, final_price, final_distance, cancellation_reason FROM service_requests WHERE id = ?"
            val stmtCheck = conn.prepareStatement(sqlCheck)
            stmtCheck.setInt(1, requestId)
            val rsCheck = stmtCheck.executeQuery()

            if (rsCheck.next()) {
                val statusAtual = rsCheck.getString("status")
                val providerId = rsCheck.getInt("assigned_provider_id")

                if (statusAtual == "accepted" && providerId != 0) {
                    // 2. Se aceitou, busca o pacote completo com JOIN
                    val sqlJoin = """
                        SELECT 
                            u.user_name AS oficina_nome, -- 🔥 CORRIGIDO DE u.name PARA u.user_name
                            p.profile_photo_url, 
                            v.name AS veiculo_nome, 
                            v.plate AS veiculo_placa
                        FROM users u
                        LEFT JOIN provider_profiles p ON u.user_id = p.provider_id
                        -- Pega o primeiro veículo ativo da oficina
                        LEFT JOIN provider_vehicles v ON u.user_id = v.provider_id AND v.is_active = 1
                        WHERE u.user_id = ?
                        LIMIT 1
                    """.trimIndent()

                    val stmtJoin = conn.prepareStatement(sqlJoin)
                    stmtJoin.setInt(1, providerId)
                    val rsJoin = stmtJoin.executeQuery()

                    if(rsJoin.next()){
                        val detalhes = OficinaDetalhesPolling(
                            nome = rsJoin.getString("oficina_nome") ?: "Oficina Parceira",
                            fotoPerfil = rsJoin.getString("profile_photo_url"),
                            valorFinal = rsCheck.getDouble("final_price"),
                            distanciaKm = rsCheck.getDouble("final_distance"),
                            nomeVeiculo = rsJoin.getString("veiculo_nome"),
                            placaVeiculo = rsJoin.getString("veiculo_placa")
                        )
                        resposta = PollingStatusResponse("accepted", null, detalhes)
                    } else {
                        val detalhesBasicos = OficinaDetalhesPolling("Oficina Parceira", null, rsCheck.getDouble("final_price"), rsCheck.getDouble("final_distance"), null, null)
                        resposta = PollingStatusResponse("accepted", null, detalhesBasicos)
                    }

                } else if (statusAtual == "canceled") {
                    resposta = PollingStatusResponse("canceled", rsCheck.getString("cancellation_reason"), null)
                } else {
                    resposta = PollingStatusResponse(statusAtual, null, null)
                }
            }
        }
    } catch (e: Exception) {
        println("Erro no Polling: ${e.message}")
    }
    return resposta
}

fun aceitarPedidoBanco(dados: AceitarPedidoRequest): Boolean {
    return try {
        DatabaseConfig.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1. Grava os dados finais do pedido
                val sqlRequest = """
                    UPDATE service_requests 
                    SET status = 'accepted', 
                        assigned_provider_id = ?, 
                        final_price = ?, 
                        final_distance = ? 
                    WHERE id = ? AND status = 'searching'
                """.trimIndent()

                val stmt = conn.prepareStatement(sqlRequest)
                stmt.setInt(1, dados.providerId)
                stmt.setDouble(2, dados.price)
                stmt.setDouble(3, dados.distance)
                stmt.setInt(4, dados.requestId)

                val linhasAfetadas = stmt.executeUpdate()

                if (linhasAfetadas > 0) {
                    // 2. Atualiza a tabela de convites (Matches)
                    val sqlMatch = """
                        UPDATE service_matches 
                        SET status = 'accepted' 
                        WHERE request_id = ? AND provider_id = ?
                    """.trimIndent()

                    val stmtMatch = conn.prepareStatement(sqlMatch)
                    stmtMatch.setInt(1, dados.requestId)
                    stmtMatch.setInt(2, dados.providerId)
                    stmtMatch.executeUpdate()

                    conn.commit()
                    true
                } else {
                    conn.rollback()
                    false
                }
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    } catch (e: Exception) {
        println("Erro ao aceitar pedido no MySQL: ${e.message}")
        false
    }
}