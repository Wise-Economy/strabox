package com.wiseeconomy.strabox

import pureconfig.ConfigSource
import zio.{ Has, ZIO, ZLayer }
import pureconfig.generic.auto._

object config {

  type ConfigProvider = Has[Config]

  final case class Config(
    db: DBConfig,
    app: AppConfig)

  object ConfigProvider {

    val live: ZLayer[Any, IllegalStateException, Has[Config]] =
      ZLayer.fromEffect {
        ZIO
          .fromEither(ConfigSource.default.load[Config])
          .mapError(
            failures =>
              new IllegalStateException(
                s"Error loading configuration: $failures"
              )
          )
      }
  }

  final case class DBConfig(
    driver: String,
    url: String,
    user: String,
    password: String)

  type DbConfigProvider = Has[DBConfig]

  object DbConfigProvider {

    val fromConfig: ZLayer[ConfigProvider, Nothing, DbConfigProvider] =
      ZLayer.fromService(_.db)
  }

  type AppConfigProvider = Has[AppConfig]

  object AppConfigProvider {

    val fromConfig: ZLayer[ConfigProvider, Nothing, AppConfigProvider] =
      ZLayer.fromService(_.app)
  }

  final case class AppConfig(port: Int)

}
