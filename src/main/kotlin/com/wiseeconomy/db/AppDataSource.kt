package com.wiseeconomy.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.wiseeconomy.DBConfig
import java.net.URI
import javax.sql.DataSource

object AppDataSource {

    fun fromDatabaseUrl(dbUrl: String): DataSource {

        val dbUri = URI(dbUrl)
        val username: String = dbUri.userInfo.split(":")[0]
        val password: String = dbUri.userInfo.split(":")[1]
        val host: String = dbUri.host
        val port: Int = dbUri.port

        val databaseName: String = when {
            dbUri.path.startsWith("/") -> dbUri.path.drop(1)
            else -> dbUri.path
        }

        val config = HikariConfig ().apply {
            this.jdbcUrl = "jdbc:postgresql://$host:$port/$databaseName"
            this.driverClassName = "org.postgresql.Driver"
            this.username = username
            this.password = password
            this.maximumPoolSize = 10
        }

        return HikariDataSource(config)
    }

    fun fromConfig(cfg: DBConfig): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = cfg.jdbcUrl
            driverClassName = cfg.driverClassName
            username = cfg.username
            password = cfg.password
            maximumPoolSize = cfg.maximumPoolSize
        }
        return HikariDataSource(config)
    }

}