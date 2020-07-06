package com.wiseeconomy.strabox.services.verifier

import com.wiseeconomy.strabox.domain.Email
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import zio._
import org.http4s.implicits._
import zio.interop.catz._
import io.circe.generic.auto._
import org.http4s.Method._
import cats.implicits._
import io.circe.Decoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

final class GoogleAccessVerifier(client: Client[Task])
    extends Http4sClientDsl[Task] {

  private val googleAccessVerifier = new AccessVerifier.Service {

    override def verify(
      accessToken: String,
      email: Email
    ): Task[Boolean] = {

      implicit def circeJsonDecoder[A](
        implicit
        decoder: Decoder[A]
      ): EntityDecoder[Task, A] =
        jsonOf[Task, A]

      client
        .expect[Email](
          GET(
            uri"https://www.googleapis.com/oauth2/v1/tokeninfo"
              .withQueryParam("access_token", accessToken)
          )
        )
        .map(_.value === email.value)
    }
  }
}

object GoogleAccessVerifier {

  def live: ZLayer[HttpClient, Nothing, AccessVerifier] = ZLayer.fromManaged {
    for {
      client <- ZIO.service[Client[Task]].toManaged_
    } yield new GoogleAccessVerifier(client).googleAccessVerifier
  }
}
