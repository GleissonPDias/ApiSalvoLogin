package com.example.database

import com.example.models.PedidoSocorroRequest
import com.example.models.PedidoSocorroResponse

fun solicitarSocorroRadar(pedido: PedidoSocorroRequest): PedidoSocorroResponse {
    return try {
        val raioBuscaMetros = 15.0 * 1000 // 15 KM

        DatabaseConfig.getConnection().use { conn ->
            conn.autoCommit = false

            try {
                // 1. CRIA O PEDIDO
                val sqlInsertRequest = """
                    INSERT INTO service_requests (customer_id, description, location_lat, location_lng, status)
                    VALUES (?, ?, ?, ?, 'searching') RETURNING id
                """
                val stmtRequest = conn.prepareStatement(sqlInsertRequest)
                stmtRequest.setInt(1, pedido.customerId)
                stmtRequest.setString(2, pedido.description)
                stmtRequest.setDouble(3, pedido.latitude)
                stmtRequest.setDouble(4, pedido.longitude)

                val rsRequest = stmtRequest.executeQuery()
                rsRequest.next()
                val novoRequestId = rsRequest.getInt("id")

                // 2. O RADAR POSTGIS
                val sqlRadar = """
                    SELECT u.user_id 
                    FROM users u
                    JOIN provider_profiles prof ON u.user_id = prof.provider_id
                    JOIN provider_services ps ON u.user_id = ps.provider_id
                    WHERE u.user_role = 'provider' 
                      AND prof.is_receiving_requests = TRUE
                      AND ps.service_type = ? 
                      AND ps.is_active = TRUE
                      AND ST_DWithin(
                          geography(ST_MakePoint(u.longitude, u.latitude)), 
                          geography(ST_MakePoint(?, ?)), 
                          ? 
                      )
                """
                val stmtRadar = conn.prepareStatement(sqlRadar)
                stmtRadar.setString(1, pedido.serviceType)
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

                PedidoSocorroResponse(true, "Pedido criado! Procurando socorro...", novoRequestId, oficinasEncontradas.size)

            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    } catch (e: Exception) {
        PedidoSocorroResponse(false, "Erro no Radar: ${e.message}")
    }
}