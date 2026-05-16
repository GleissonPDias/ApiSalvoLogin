package com.example.models

data class PedidosResponse(
    val id: Int,
    val customer_id: Int,
    val service_type: String,
    val description: String,
    val vehicle_info: String?,
    val status: String,
    val assigned_provider_id: Int?,
    val prestador_nome: String?,
    val final_price: Double?,
    val final_distance: Double?,
    val destino_address: String?,
    val created_at: String
)

data class PedidoPendenteResponse(
    val matchId: Int, // ID do convite na tabela service_matches
    val requestId: Int,
    val serviceType: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val clienteNome: String,
    val clienteTelefone: String,
    // Dados do Veículo que vieram do JOIN:
    val veiculoMarca: String,
    val veiculoModelo: String,
    val veiculoPlaca: String,
    val veiculoAno: String
)