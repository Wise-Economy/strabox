package com.wiseeconomy.strabox

import cats.effect._
import com.wiseeconomy.http.Routes
import fs2.Stream.Compiler._
import com.wiseeconomy.strabox.Layers.AppEnv
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio._
import org.http4s._
import org.http4s.implicits._
import zio.clock.Clock
import zio.interop.catz._

object Main extends App {

  type AppTask[A] =
    RIO[AppEnv with Clock, A]

  override def run(args: List[String]): URIO[zio.ZEnv, zio.ExitCode] = {
    val prog = for {
      cfg    <- ZIO.service[config.Config]
      _      <- logging.log.info(s"Starting with $cfg")
      appCfg = cfg.app

      httpApp = Router[AppTask](
        "/" -> Routes.routes
      ).orNotFound

      _ <- runHttp(httpApp, appCfg.port)
    } yield zio.ExitCode.success

    prog
      .provideSomeLayer[ZEnv](Layers.live.appLayer)
      .orDie
  }

  def runHttp[R <: Clock](
    httpApp: HttpApp[RIO[R, *]],
    port: Int
  ): ZIO[R, Throwable, Unit] = {
    type Task[A] = RIO[R, A]
    ZIO.runtime[R].flatMap { implicit rts =>
      BlazeServerBuilder(rts.platform.executor.asEC)
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(CORS(httpApp))
        .serve
        .compile[Task, Task, cats.effect.ExitCode]
        .drain
    }
  }
}
