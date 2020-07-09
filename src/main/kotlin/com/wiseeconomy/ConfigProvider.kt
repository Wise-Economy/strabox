package com.wiseeconomy

import com.sksamuel.hoplite.ConfigLoader

data class AppConfig(val port: Int)

data class DBConfig(val jdbcUrl: String,
                    val driverClassName: String,
                    val username: String,
                    val password: String,
                    val maximumPoolSize: Int)

data class Config(val db: DBConfig, val app: AppConfig) {
    companion object {
        val loadConfig: Config = ConfigLoader().loadConfigOrThrow("/application.conf")
    }
}