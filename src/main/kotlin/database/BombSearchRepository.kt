package com.example.database

import com.example.models.PartidaBombSearchRequest
import com.example.models.PartidaBombSearchResponse // Importante importar a Response

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

    // NOVA FUNÇÃO: Busca o histórico no banco de dados
    fun buscarHistorico(): List<PartidaBombSearchResponse> {
        // Puxa as partidas ordenando da maior pontuação para a menor (Ranking)
        val query = "SELECT id, nome_jogador, pontuacao, data_partida FROM bombsearch_partidas ORDER BY pontuacao DESC LIMIT 50"
        val listaHistorico = mutableListOf<PartidaBombSearchResponse>()

        return try {
            DatabaseConfig.getConnection().use { conn ->
                val statement = conn.prepareStatement(query)
                val resultSet = statement.executeQuery()

                // Percorre linha por linha do resultado do banco
                while (resultSet.next()) {
                    val partida = PartidaBombSearchResponse(
                        id = resultSet.getInt("id"),
                        nomeJogador = resultSet.getString("nome_jogador"),
                        pontuacao = resultSet.getInt("pontuacao"),
                        dataPartida = resultSet.getString("data_partida") ?: ""
                    )
                    listaHistorico.add(partida)
                }
            }
            listaHistorico // Retorna a lista preenchida
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // Se der erro, retorna uma lista vazia para não travar o app
        }
    }
}