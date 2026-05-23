package com.example.models


data class PartidaBombSearchRequest(
    val nomeJogador: String,
    val pontuacao: Int
)


data class PartidaBombSearchResponse(
    val id: Int,
    val nomeJogador: String,
    val pontuacao: Int,
    val dataPartida: String
)
