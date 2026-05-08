package com.example.models

data class PedidosResponse(
    val servico: String,
    val status: String,
    val data_hora: String,
    val prestador: String,
    val preco: Double
)