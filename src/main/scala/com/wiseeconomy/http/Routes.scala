package com.wiseeconomy.http

import java.util.UUID

import com.wiseeconomy.strabox.db
import com.wiseeconomy.strabox.db.DBAccess
import com.wiseeconomy.strabox.domain.{
  AuthToken,
  Email,
  ExpiredAccessToken,
  MissingAccessTokenHeader,
  User,
  UserId,
  UserNotFound
}
import com.wiseeconomy.strabox.services.verifier
import com.wiseeconomy.strabox.services.verifier.AccessVerifier
import io.circe.{ Decoder, Encoder }
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import zio._
import io.circe.generic.auto._
import zio.interop.catz._

import scala.util.Try

case class Error(msg: String)

object Routes {

  def routes[R <: AccessVerifier with DBAccess]: HttpRoutes[RIO[R, ?]] = {

    type Task[A] = RIO[R, A]

    val dsl: Http4sDsl[Task] = Http4sDsl[Task]
    import dsl._

    implicit def circeJsonDecoder[A](
      implicit
      decoder: Decoder[A]
    ): EntityDecoder[Task, A] =
      jsonOf[Task, A]

    implicit def circeJsonEncoder[A](
      implicit
      encoder: Encoder[A]
    ): EntityEncoder[Task, A] =
      jsonEncoderOf[Task, A]

    def getHeader(
      req: Request[Task],
      name: String
    ) =
      req.headers.get(CaseInsensitiveString(name)) match {
        case Some(header) => Task.succeed(header.value)
        case None         => Task.fail(MissingAccessTokenHeader)
      }

    def getAccessToken(req: Request[Task]) =
      getHeader(req, "X-Access-Token")

    def getAuthToken(req: Request[Task]): Task[AuthToken] =
      getHeader(req, "X-Auth-Token").flatMap { str =>
        Task.fromTry(Try(AuthToken(UUID.fromString(str))))
      }

    def verifyOrHalt(
      accessToken: String,
      email: Email
    ) =
      verifier.verify(accessToken, email).flatMap { verified =>
        if (verified) Task.succeed(()) else Task.fail(ExpiredAccessToken)
      }

    HttpRoutes.of[Task] {
      case req @ POST -> Root / "api" / "v1" / "userExists" =>
        req.decode[Email] { email =>
          val result = for {
            accessToken <- getAccessToken(req)
            _           <- verifyOrHalt(accessToken, email)
            emailExists <- db.exists(email)
          } yield emailExists

          result.flatMap(exists => if (exists) NoContent() else NotFound())
        }

      case req @ POST -> Root / "api" / "v1" / "register" =>
        req.decode[User] { user =>
          val result = for {
            accessToken <- getAccessToken(req)
            _           <- verifyOrHalt(accessToken, user.email)
            _           <- db.saveUser(user)
          } yield ()

          result.flatMap(_ => Created())
        }

      case req @ POST -> Root / "api" / "v1" / "authToken" =>
        req.decode[Email] { email =>
          val result = for {
            accessToken <- getAccessToken(req)
            _           <- verifyOrHalt(accessToken, email)
            token       <- db.getOrCreateAuthToken(email)
          } yield token
          result.flatMap(token => Ok(token))
        }

      case req @ GET -> Root / "api" / "v1" / "userProfile" =>
        val result = for {
          token     <- getAuthToken(req)
          userIdOpt <- db.getUserId(token)
          userId <- userIdOpt.fold[Task[UserId]](Task.fail(UserNotFound))(
                     Task.succeed(_)
                   )
          userOpt <- db.getUser(userId)
          user <- userOpt.fold[Task[User]](Task.fail(UserNotFound))(
                   Task.succeed(_)
                 )
        } yield user
        result.flatMap(user => Ok(user))

      case req @ GET -> Root / "api" / "v1" / "logout" =>
        val action = for {
          token <- getAuthToken(req)
          _     <- db.invalidate(token)
        } yield ()
        action *> NoContent()
    }
  }
}
