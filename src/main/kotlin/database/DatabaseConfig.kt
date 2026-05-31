package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object DatabaseConfig {

    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig()

        // Suas credenciais
        config.jdbcUrl = "jdbc:mysql://thyagoquintas.com.br:3306/engenharia_339"
        config.username = "engenharia_339"
        config.password = "capivara"
        config.driverClassName = "com.mysql.cj.jdbc.Driver"

        // 🔥 O Segredo da Velocidade (Configurações do Pool)
        config.maximumPoolSize = 10       // Máximo de conexões simultâneas que ficam abertas
        config.minimumIdle = 2            // Mantém sempre 2 prontas para uso imediato
        config.idleTimeout = 30000        // Fecha conexões ociosas após 30 segundos
        config.connectionTimeout = 10000  // Tempo máximo esperando uma conexão (10s)
        config.maxLifetime = 1800000      // Tempo de vida máximo de uma conexão (30 min) para evitar problemas no banco

        dataSource = HikariDataSource(config)
    }

    // Essa função agora é instantânea! Ela pega uma conexão já aberta na piscina.
    fun getConnection(): Connection {
        return dataSource.connection
    }
}