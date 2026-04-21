package com.example.models

data class RegisterRequest(
    val nome: String,
    val email: String,
    val cpf: String,
    val password: String,
    val telefone: String = "Não informado",
    val role: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val sucesso: Boolean,
    val message: String,
    val userId: Int? = null,
    val nome: String? = null,
    val role: String? = null
)