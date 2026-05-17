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
            // Query atualizada trazendo foto de perfil e dados do guincho
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
                    u.user_banner AS prestador_foto, -- Tabela provider_profiles
                    pv.name AS veiculo_prestador_nome,   -- Tabela provider_vehicles
                    pv.plate AS veiculo_prestador_placa, -- Tabela provider_vehicles
                    sr.final_price, 
                    sr.final_distance, 
                    sr.destino_address, 
                    sr.created_at
                FROM service_requests sr
                LEFT JOIN users u ON sr.assigned_provider_id = u.user_id
                LEFT JOIN customer_vehicles cv ON sr.vehicle_id = cv.id
                LEFT JOIN provider_profiles pp ON sr.assigned_provider_id = pp.provider_id
                LEFT JOIN provider_vehicles pv ON sr.assigned_provider_id = pv.provider_id AND pv.is_active = 1
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
                        created_at = rs.getString("created_at") ?: "",
                        // 🚀 MAPEAMENTO DOS NOVOS CAMPOS:
                        prestador_foto = rs.getString("prestador_foto"),
                        veiculo_prestador_nome = rs.getString("veiculo_prestador_nome"),
                        veiculo_prestador_placa = rs.getString("veiculo_prestador_placa")
                    )
                )
            }
            listaPedidos
        }
    } catch (e: Exception) {
        println("Erro ao buscar pedidos com dados extras: ${e.message}")
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

fun buscarHistoricoDaOficina(providerId: Int): List<PedidosResponse> {
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
                    c.user_name AS prestador_nome, -- 🔥 TRUQUE: Pegamos o nome do CLIENTE na tabela users
                    u.user_banner AS prestador_foto, 
                    pv.name AS veiculo_prestador_nome,
                    pv.plate AS veiculo_prestador_placa,
                    sr.final_price, 
                    sr.final_distance, 
                    sr.destino_address, 
                    sr.created_at
                FROM service_requests sr
                LEFT JOIN users u ON sr.assigned_provider_id = u.user_id -- Perfil da Oficina
                LEFT JOIN users c ON sr.customer_id = c.user_id          -- Perfil do Cliente
                LEFT JOIN customer_vehicles cv ON sr.vehicle_id = cv.id
                LEFT JOIN provider_vehicles pv ON sr.assigned_provider_id = pv.provider_id AND pv.is_active = 1
                WHERE sr.assigned_provider_id = ? AND sr.status IN ('accepted', 'in_progress', 'completed')
                ORDER BY sr.created_at DESC
            """.trimIndent()

            val statement = conn.prepareStatement(sql)
            statement.setInt(1, providerId)
            val rs = statement.executeQuery()

            while (rs.next()) {
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
                        assigned_provider_id = rs.getInt("assigned_provider_id"),

                        // 🔥 TRUQUE: O 'prestador_nome' agora devolve o NOME DO CLIENTE para a tela da oficina!
                        prestador_nome = rs.getString("prestador_nome"),

                        final_price = finalPriceSafe,
                        final_distance = finalDistanceSafe,
                        destino_address = rs.getString("destino_address"),
                        created_at = rs.getString("created_at") ?: "",
                        prestador_foto = rs.getString("prestador_foto"),
                        veiculo_prestador_nome = rs.getString("veiculo_prestador_nome"),
                        veiculo_prestador_placa = rs.getString("veiculo_prestador_placa")
                    )
                )
            }
            listaPedidos
        }
    } catch (e: Exception) {
        println("Erro ao buscar histórico da oficina: ${e.message}")
        emptyList()
    }
}

fun verificarStatusDoPedidoBanco(requestId: Int): PollingStatusResponse {
    // Valor padrão caso o pedido nem exista no banco
    var resposta = PollingStatusResponse(status = "unknown")

    try {
        DatabaseConfig.getConnection().use { conn ->
            // 1. Checa o status atual do pedido na tabela principal
            val sqlCheck = "SELECT status, assigned_provider_id, final_price, final_distance, cancellation_reason FROM service_requests WHERE id = ?"
            val stmtCheck = conn.prepareStatement(sqlCheck)
            stmtCheck.setInt(1, requestId)
            val rsCheck = stmtCheck.executeQuery()

            if (rsCheck.next()) {
                val statusAtual = rsCheck.getString("status")
                val providerId = rsCheck.getInt("assigned_provider_id")

                if (statusAtual == "accepted" && providerId != 0) {

                    // 🔥 PASSO SEGURO: Define a resposta como 'accepted' IMEDIATAMENTE.
                    // Mesmo que as tabelas de fotos ou veículos falhem, o cliente NÃO FICA PRESO!
                    val detalhesBasicos = OficinaDetalhesPolling(
                        nome = "Oficina Confirmada",
                        fotoPerfil = null,
                        valorFinal = rsCheck.getDouble("final_price"),
                        distanciaKm = rsCheck.getDouble("final_distance"),
                        nomeVeiculo = null,
                        placaVeiculo = null
                    )
                    resposta = PollingStatusResponse("accepted", null, detalhesBasicos)

                    // 2. Tenta buscar os dados estéticos (nome e veículo), mas isolado para não quebrar o fluxo
                    try {
                        val sqlJoin = """
                            SELECT 
                                u.user_name AS oficina_nome, 
                                v.name AS veiculo_nome, 
                                v.plate AS veiculo_placa
                            FROM users u
                            LEFT JOIN provider_vehicles v ON u.user_id = v.provider_id AND v.is_active = 1
                            WHERE u.user_id = ?
                            LIMIT 1
                        """.trimIndent()

                        val stmtJoin = conn.prepareStatement(sqlJoin)
                        stmtJoin.setInt(1, providerId)
                        val rsJoin = stmtJoin.executeQuery()

                        if (rsJoin.next()) {
                            val detalhesCompletos = OficinaDetalhesPolling(
                                nome = rsJoin.getString("oficina_nome") ?: "Oficina Confirmada",
                                fotoPerfil = null, // Deixado nulo temporariamente para evitar erros com a tabela de perfis
                                valorFinal = rsCheck.getDouble("final_price"),
                                distanciaKm = rsCheck.getDouble("final_distance"),
                                nomeVeiculo = rsJoin.getString("veiculo_nome"),
                                placaVeiculo = rsJoin.getString("veiculo_placa")
                            )
                            resposta = PollingStatusResponse("accepted", null, detalhesCompletos)
                        }
                    } catch (eInner: Exception) {
                        // Se colunas como 'v.name' ou tabelas falharem, o Ktor avisa o terminal, mas não quebra o app do cliente
                        println("⚠️ Aviso no Polling (Detalhes Oficina): ${eInner.message}")
                    }

                } else if (statusAtual == "canceled") {
                    resposta = PollingStatusResponse("canceled", rsCheck.getString("cancellation_reason"), null)
                } else {
                    // Retorna o status atual real (ex: 'searching')
                    resposta = PollingStatusResponse(statusAtual, null, null)
                }
            }
        }
    } catch (e: Exception) {
        println("❌ Erro grave no verificarStatusDoPedidoBanco: ${e.message}")
        e.printStackTrace()
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