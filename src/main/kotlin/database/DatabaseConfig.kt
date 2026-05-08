package com.example.database

import java.sql.Connection
import java.sql.DriverManager


object DatabaseConfig {
    fun getConnection(): Connection {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val url = "jdbc:mysql://www.thyagoquintas.com.br:3306/engenharia_339"
        val user = "engenharia_339"
        val password = "capivara"

        return DriverManager.getConnection(url, user, password)
    }
}
