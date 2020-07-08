package com.wiseeconomy.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.wiseeconomy.DBConfig
import javax.sql.DataSource

object AppDataSource {

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