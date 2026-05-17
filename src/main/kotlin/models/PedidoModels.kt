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

data class PollingStatusResponse(
    val status: String, // 'searching', 'accepted' ou 'canceled'
    val razaoCancelamento: String? = null,
    val detalhesOficina: OficinaDetalhesPolling? = null
)

data class OficinaDetalhesPolling(
    val nome: String,
    val fotoPerfil: String?, // Ex: URL da imagem na tabela provider_profiles
    val valorFinal: Double,
    val distanciaKm: Double,
    val nomeVeiculo: String?, // Vem de provider_vehicles.name
    val placaVeiculo: String? // Vem de provider_vehicles.plate
)

data class AceitarPedidoRequest(
    val requestId: Int,
    val providerId: Int,
    val price: Double,
    val distance: Double
)