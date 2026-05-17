package com.example.models

data class PedidoSocorroRequest(
    val customerId: Int,
    val latitude: Double,
    val longitude: Double,
    val serviceType: String,
    val vehicleId: Int,
    val description: String,
)

data class ProviderMatchDetail(
    val providerId: Int,
    val preco: Double,
    val distanciaKm: Double,
    val minutosEstimados: Int
)

data class PedidoSocorroResponse(
    val sucesso: Boolean,
    val mensagem: String,
    val requestId: Int? = null,
    val mecanicosNotificados: Int = 0,
    val prestadoresMatch: List<ProviderMatchDetail>? = null
)