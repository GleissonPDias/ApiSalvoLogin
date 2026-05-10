package com.example.models

data class ProviderServiceResponse(
    val id: Int,
    val provider_id: Int,
    val service_type: String,
    val base_price: Double,
    val price_per_km: Double,
    val is_active: Boolean
)
