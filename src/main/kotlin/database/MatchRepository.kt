package com.example.database

import com.example.models.PedidoSocorroRequest
import com.example.models.PedidoSocorroResponse
import com.example.models.ProviderMatchDetail // NOVO: Importa o modelo detalhado
import java.sql.Statement

fun solicitarSocorroRadar(pedido: PedidoSocorroRequest): PedidoSocorroResponse {
    return try {
        val raioBuscaMetros = 15.0 * 1000 // 15 KM

        DatabaseConfig.getConnection().use { conn ->
            conn.autoCommit = false

            try {
                // 1. CRIA O PEDIDO NO BANCO
                val sqlInsertRequest = """
                    INSERT INTO service_requests (customer_id, vehicle_id, service_type, description, location_lat, location_lng, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'searching')
                """.trimIndent()

                val stmtRequest = conn.prepareStatement(sqlInsertRequest, Statement.RETURN_GENERATED_KEYS)
                stmtRequest.setInt(1, pedido.customerId)
                stmtRequest.setInt(2, pedido.vehicleId)
                stmtRequest.setString(3, pedido.serviceType)
                stmtRequest.setString(4, pedido.description)
                stmtRequest.setDouble(5, pedido.latitude)
                stmtRequest.setDouble(6, pedido.longitude)
                stmtRequest.executeUpdate()

                val rsRequest = stmtRequest.generatedKeys
                var novoRequestId = -1
                if (rsRequest.next()) {
                    novoRequestId = rsRequest.getInt(1)
                }

                if (novoRequestId == -1) throw Exception("Falha ao gerar o ID do pedido no MySQL.")

                // 2. O RADAR DINÂMICO (Calcula distância e puxa preço)
                val sqlRadar = """
                    SELECT u.user_id, ps.preco,
                           ST_Distance_Sphere(POINT(u.longitude, u.latitude), POINT(?, ?)) AS distancia_metros
                    FROM users u
                    JOIN provider_profiles prof ON u.user_id = prof.provider_id
                    JOIN provider_services ps ON u.user_id = ps.provider_id
                    WHERE u.user_role = 'provider' 
                      AND prof.is_receiving_requests = TRUE
                      AND ps.service_type = ? 
                      AND ps.is_active = TRUE
                      AND ST_Distance_Sphere(POINT(u.longitude, u.latitude), POINT(?, ?)) <= ?
                """.trimIndent()

                val stmtRadar = conn.prepareStatement(sqlRadar)
                stmtRadar.setDouble(1, pedido.longitude)
                stmtRadar.setDouble(2, pedido.latitude)
                stmtRadar.setString(3, pedido.serviceType)
                stmtRadar.setDouble(4, pedido.longitude)
                stmtRadar.setDouble(5, pedido.latitude)
                stmtRadar.setDouble(6, raioBuscaMetros)

                val rsRadar = stmtRadar.executeQuery()

                // NOVO: Agora a lista guarda um objeto rico, não mais só um número de ID
                val oficinasEncontradas = mutableListOf<ProviderMatchDetail>()

                while (rsRadar.next()) {
                    val id = rsRadar.getInt("user_id")
                    val precoDoServico = rsRadar.getDouble("preco")
                    val distanciaEmMetros = rsRadar.getDouble("distancia_metros")

                    // Converte metros para KM com uma casa decimal e estima o tempo
                    val distanciaKm = Math.round((distanciaEmMetros / 1000.0) * 10.0) / 10.0
                    val minutosEstimados = Math.max(2, (distanciaKm * 2.5).toInt())

                    oficinasEncontradas.add(
                        ProviderMatchDetail(id, precoDoServico, distanciaKm, minutosEstimados)
                    )
                }

                // 3. CRIA OS MATCHES (Convites)
                if (oficinasEncontradas.isNotEmpty()) {
                    val sqlInsertMatch = "INSERT INTO service_matches (request_id, provider_id, status) VALUES (?, ?, 'pending')"
                    val stmtMatch = conn.prepareStatement(sqlInsertMatch)

                    for (oficina in oficinasEncontradas) {
                        stmtMatch.setInt(1, novoRequestId)
                        stmtMatch.setInt(2, oficina.providerId) // Atualizado para puxar do objeto
                        stmtMatch.addBatch()
                    }
                    stmtMatch.executeBatch()
                }

                conn.commit()

                return PedidoSocorroResponse(
                    sucesso = true,
                    mensagem = "Pedido criado! Procurando socorro...",
                    requestId = novoRequestId,
                    mecanicosNotificados = oficinasEncontradas.size,
                    prestadoresMatch = oficinasEncontradas // NOVO: Devolve a lista rica
                )

            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    } catch (e: Exception) {
        return PedidoSocorroResponse(
            sucesso = false,
            mensagem = "Erro no Radar: ${e.message}",
            requestId = null,
            mecanicosNotificados = 0,
            prestadoresMatch = null
        )
    }
}