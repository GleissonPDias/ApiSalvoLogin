package com.example.models

data class PedidoSocorroRequest(
    val customerId: Int,
    val latitude: Double,
    val longitude: Double,
    val serviceType: String,
    val vehicleId: Int,
    val description: String,
)

data class PedidoSocorroResponse(
    val sucesso: Boolean,
    val message: String,
    val requestId: Int? = null,
    val mecanicosNotificados: Int = 0
)