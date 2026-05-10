package com.example.models

data class ProviderVehicleResponse(
    val id: Int,
    val provider_id: Int,
    val name: String,
    val plate: String,
    val status: String,
    val vehicle_photo: String?, // Pode ser nulo se cadastrar sem foto
    val is_active: Boolean
)