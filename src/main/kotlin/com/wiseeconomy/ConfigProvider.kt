package com.wiseeconomy

import com.sksamuel.hoplite.ConfigLoader

data class DBConfig(val jdbcUrl: String,
                    val driverClassName: String,
                    val username: String,
                    val password: String,
                    val maximumPoolSize: Int)

data class Config(val db: DBConfig) {
    companion object {
        val loadConfig: Config = ConfigLoader().loadConfigOrThrow("/application.conf")
    }
}