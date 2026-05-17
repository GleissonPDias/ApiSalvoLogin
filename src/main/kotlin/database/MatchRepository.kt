package com.example.database

import com.example.models.PedidoSocorroRequest
import com.example.models.PedidoSocorroResponse
import java.sql.Statement // IMPORTANTE: Necessário para o RETURN_GENERATED_KEYS no MySQL

fun solicitarSocorroRadar(pedido: PedidoSocorroRequest): PedidoSocorroResponse {
    return try {
        val raioBuscaMetros = 15.0 * 1000 // 15 KM

        DatabaseConfig.getConnection().use { conn ->
            conn.autoCommit = false

            try {
                // 1. CRIA O PEDIDO (Corrigido)
                val sqlInsertRequest = """
                    INSERT INTO service_requests (customer_id, vehicle_id, service_type, description, location_lat, location_lng, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'searching')
                """.trimIndent()

                // Avisamos o MySQL que queremos capturar o ID (Auto Increment) gerado
                val stmtRequest = conn.prepareStatement(sqlInsertRequest, Statement.RETURN_GENERATED_KEYS)

                stmtRequest.setInt(1, pedido.customerId)
                stmtRequest.setInt(2, pedido.vehicleId)
                stmtRequest.setString(3, pedido.serviceType)
                stmtRequest.setString(4, pedido.description)
                stmtRequest.setDouble(5, pedido.latitude)
                stmtRequest.setDouble(6, pedido.longitude)

                // Usamos executeUpdate() para operações INSERT/UPDATE/DELETE
                stmtRequest.executeUpdate()

                // Captura o ID gerado pelo MySQL
                val rsRequest = stmtRequest.generatedKeys
                var novoRequestId = -1
                if (rsRequest.next()) {
                    novoRequestId = rsRequest.getInt(1)
                }

                if (novoRequestId == -1) {
                    throw Exception("Falha ao gerar o ID do pedido no MySQL.")
                }

                // 2. O RADAR (Adaptado para MySQL Espacial)
                val sqlRadar = """
                    SELECT u.user_id 
                    FROM users u
                    JOIN provider_profiles prof ON u.user_id = prof.provider_id
                    JOIN provider_services ps ON u.user_id = ps.provider_id
                    WHERE u.user_role = 'provider' 
                      AND prof.is_receiving_requests = TRUE
                      AND ps.service_type = ? 
                      AND ps.is_active = TRUE
                      -- Substituímos o PostGIS pelo ST_Distance_Sphere do MySQL
                      AND ST_Distance_Sphere(POINT(u.longitude, u.latitude), POINT(?, ?)) <= ?
                """.trimIndent()

                val stmtRadar = conn.prepareStatement(sqlRadar)
                stmtRadar.setString(1, pedido.serviceType)
                // Lembrete: O POINT no MySQL recebe primeiro a Longitude (X), depois a Latitude (Y)
                stmtRadar.setDouble(2, pedido.longitude)
                stmtRadar.setDouble(3, pedido.latitude)
                stmtRadar.setDouble(4, raioBuscaMetros)

                val rsRadar = stmtRadar.executeQuery()
                val oficinasEncontradas = mutableListOf<Int>()

                while (rsRadar.next()) {
                    oficinasEncontradas.add(rsRadar.getInt("user_id"))
                }

                // 3. CRIA OS MATCHES (Convites)
                if (oficinasEncontradas.isNotEmpty()) {
                    val sqlInsertMatch = "INSERT INTO service_matches (request_id, provider_id, status) VALUES (?, ?, 'pending')"
                    val stmtMatch = conn.prepareStatement(sqlInsertMatch)

                    for (oficinaId in oficinasEncontradas) {
                        stmtMatch.setInt(1, novoRequestId)
                        stmtMatch.setInt(2, oficinaId)
                        stmtMatch.addBatch()
                    }
                    stmtMatch.executeBatch()
                }

                conn.commit()

                // MODIFICAÇÃO: Incluído o parâmetro idsPrestadores recebendo a lista do Radar
                return PedidoSocorroResponse(
                    sucesso = true,
                    mensagem = "Pedido criado! Procurando socorro...",
                    requestId = novoRequestId,
                    mecanicosNotificados = oficinasEncontradas.size,
                    idsPrestadores = oficinasEncontradas
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
            idsPrestadores = null
        )
    }
}