package com.wiseeconomy.strabox.services.verifier

import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.{Task, TaskManaged, ZIO, ZLayer}
import zio.interop.catz._

object HttpClient {

  def http4sClient: ZLayer[Any, Throwable, HttpClient] = {
    val makeHttpClient: TaskManaged[Client[Task]] =
      ZIO
        .runtime[Any]
        .toManaged_
        .flatMap { implicit rts =>
          BlazeClientBuilder
            .apply[Task](rts.platform.executor.asEC)
            .resource
            .toManaged
        }
    ZLayer.fromManaged(makeHttpClient)
  }

}
