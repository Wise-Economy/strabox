package com.wiseeconomy.strabox

import com.wiseeconomy.strabox.config.{
  AppConfigProvider,
  ConfigProvider,
  DbConfigProvider
}
import com.wiseeconomy.strabox.db.DBAccess
import com.wiseeconomy.strabox.services.db.PGAccess
import com.wiseeconomy.strabox.services.verifier.{
  AccessVerifier,
  GoogleAccessVerifier,
  HttpClient
}
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging

object Layers {

  type AppEnv = ConfigProvider
    with Logging
    with Blocking
    with AppConfigProvider
    with DbConfigProvider
    with DBAccess
    with AccessVerifier

  object live {

    val loggerLayer: ZLayer[Console with Clock, Nothing, Logging] =
      Logging.console(
        format = (_, logEntry) => logEntry,
        rootLoggerName = Some("strabo")
      )

    val appLayer: ZLayer[Blocking with Console with Clock, Throwable, AppEnv] =
      (Blocking.any ++ ConfigProvider.live ++ loggerLayer) >>>
        (AppConfigProvider.fromConfig ++ DbConfigProvider.fromConfig ++ ZLayer.identity) >>>
        (PGAccess.live ++ (HttpClient.http4sClient >>> GoogleAccessVerifier.live) ++ ZLayer.identity)
  }

}
