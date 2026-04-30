package com.example.database

import com.example.models.PedidosResponse

fun buscarPedidos(userId: Int): List<PedidosResponse> {
    val listaPedidos = mutableListOf<PedidosResponse>()
    return try {
        DatabaseConfig.getConnection().use { conn ->
            val sql = "SELECT service, status, data_hora, prestador, preco FROM pedidos WHERE user_id = ?"
            val statement = conn.prepareStatement(sql)
            statement.setInt(1, userId)
            val rs = statement.executeQuery()

            while (rs.next()) {
                listaPedidos.add(
                    PedidosResponse(
                        servico = rs.getString("service"),
                        status = rs.getString("status"),
                        data_hora = rs.getString("data_hora"),
                        prestador = rs.getString("prestador"),
                        preco = rs.getDouble("preco")
                    )
                )
            }
            listaPedidos
        }
    } catch (e: Exception) {
        emptyList()
    }
}