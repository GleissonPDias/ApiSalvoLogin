package com.example.models

data class ProviderVehicleResponse(
    val id: Int,
    val provider_id: Int,
    val name: String,
    val plate: String,
    val status: String,
    val vehicle_photo: String?, // Pode ser nulo se cadastrar sem foto
    val is_active: Boolean,
    val brand: String? = null,
    val vehicle_type: String? = null,
    val maintenance_date: String? = null,
    val color: String? = null
)

data class VeiculoRequest(
    val id: Int? = null,
    val provider_id: Int? = null,
    val name: String,
    val plate: String,
    val status: String? = "Disponível",
    val brand: String? = null,
    val vehicle_type: String? = null,
    val maintenance_date: String? = null,
    val vehicle_photo: String? = null // Imagem em Base64
)