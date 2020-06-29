package controllers

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import database.{DBRepo, User}
import exceptions.{AuthenticationFailure, UserNotFound}
import io.circe.generic.auto._
import io.circe.{Encoder, Json, parser}
import io.circe.syntax._
import models.{GSignInEmail, SessionId, UserEmailAndAccessToken, UserId, UserProfile, UserRegistrationDetails}
import monix.eval.Task
import monix.execution.Scheduler
import play.api.Environment
import play.api.libs.circe.CirceBodyParsers
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class HomeController(wsClient: AhcWSClient, dbRepo: DBRepo)(implicit
                                                            val environment: Environment,
                                                            val executionContext: Scheduler,
                                                            val controllerComponents: ControllerComponents)
    extends BaseController
    with CirceBodyParsers {

  def index(): Action[AnyContent] = Action.async(parse.anyContent) { _ =>
    val userId = UUID.randomUUID()
    dbRepo
      .createUser(User.strabo(userId))
      .map(_ => Ok("Dummy strabo user created! DB connection is successful!"))
      .runToFuture
  }

  def isRegisteredUser(): Action[UserEmailAndAccessToken] = Action.async(circe.json[UserEmailAndAccessToken]) { req =>
    (for {
      verifiedUser <- verify(req.body)
      isRegistered <- dbRepo.isUserRegistered(verifiedUser.email)
    } yield if (isRegistered) Ok("") else NotFound).runToFuture
  }

  def session(): Action[UserEmailAndAccessToken] = Action.async(circe.json[UserEmailAndAccessToken]) { req =>
    (for {
      verifiedUser <- verify(req.body)
      sessionId <- dbRepo.session(verifiedUser.email)
    } yield Ok(sessionId.asJson)).onErrorRecover {
      case UserNotFound => NotFound
    }.runToFuture
  }

  def register(): Action[UserRegistrationDetails] = Action.async(circe.json[UserRegistrationDetails]) { req =>
    (for {
      verifiedUser <- verify(UserEmailAndAccessToken(req.body.email, req.body.accessToken))
      userId <- dbRepo.createUser(
        User(
          id = UUID.randomUUID(),
          name = req.body.name,
          email = verifiedUser.email,
          dob = req.body.dob,
          phoneCountryCode = req.body.phoneCountryCode,
          phoneNumber = req.body.phoneNumber,
          residenceCountryCode = req.body.residenceCountryCode,
          photoUrl = req.body.photoUrl,
          createdAt = LocalDateTime.now(ZoneId.of("UTC"))
        ))
      sessionId <- dbRepo.createSession(userId)
    } yield Ok(sessionId.asJson)).onErrorRecover {
      case UserNotFound => NotFound
    }.runToFuture
  }

  def userProfile(): Action[AnyContent] = Action.async(parse.anyContent) { req =>
    withUserId(req) { userId =>
      dbRepo
        .user(userId.id)
        .map(UserProfile.fromUser)
    }.map(profile => Ok(profile.asJson))
      .onErrorHandle {
        case AuthenticationFailure => Unauthorized
        case UserNotFound          => NotFound
      }
      .runToFuture
  }

  def logout(): Action[AnyContent] = Action.async(parse.anyContent) { req =>
    withSessionId(req) { sessionId =>
      dbRepo.invalidateSession(sessionId).map(_ => Ok(""))
    }.runToFuture
  }

  private def withSessionId[A](req: Request[_])(f: SessionId => Task[A]): Task[A] =
    req.headers.get("sessionId") match {
      case Some(sessionIdStr) =>
        Try(UUID.fromString(sessionIdStr))
          .fold(_ => Task.raiseError(AuthenticationFailure), id => Task.now(SessionId(id)))
          .flatMap(f)
      case None => Task.raiseError(AuthenticationFailure)
    }

  private def withUserId[A](req: Request[_])(f: UserId => Task[A]): Task[A] =
    withSessionId(req)(sessionId => dbRepo.userId(sessionId).flatMap(f))

  private def verify(info: UserEmailAndAccessToken): Task[GSignInEmail] =
    Task
      .fromFuture {
        wsClient
          .url(s"https://www.googleapis.com/oauth2/v1/tokeninfo")
          .addQueryStringParameters("access_token" -> info.accessToken)
          .get()
          .flatMap { res: WSResponse =>
            res.status match {
              case status if status >= 200 && status < 400 =>
                parser
                  .decode[GSignInEmail](res.body)
                  .fold(_ => Future.failed(new Exception("Json parsing failed")), Future.successful)
              case _ => Future.failed(UserNotFound)
            }
          }
      }
      .flatMap {
        case googleUser if googleUser.email == info.email => Task.now(googleUser)
        case _                                            => Task.raiseError(UserNotFound)
      }

}
