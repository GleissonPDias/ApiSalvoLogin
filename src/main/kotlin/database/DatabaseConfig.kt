package com.example.database

import java.sql.Connection
import java.sql.DriverManager


object DatabaseConfig {
    fun getConnection(): Connection {
        Class.forName("org.postgresql.Driver")
        val url = "jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require"
        val user = "postgres.rlifesgqxjgdhulthcnw"
        val password = "Senacsp@2026"

        return DriverManager.getConnection(url, user, password)
    }
}
