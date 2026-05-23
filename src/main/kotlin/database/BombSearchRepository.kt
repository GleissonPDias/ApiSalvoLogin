package com.example.database

import com.example.models.PartidaBombSearchRequest

class BombSearchRepository() {

    fun salvarPartida(partida: PartidaBombSearchRequest): Boolean {
        val query = "INSERT INTO bombsearch_partidas (nome_jogador, pontuacao) VALUES (?,?)"

        return try {
            DatabaseConfig.getConnection().use { conn ->

                val statement = conn.prepareStatement(query)

                statement.setString(1, partida.nomeJogador)
                statement.setInt(2, partida.pontuacao)

                val rowsAffected = statement.executeUpdate()
                rowsAffected > 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}