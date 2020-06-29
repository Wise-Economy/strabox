package controllers

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import database.{DBRepo, User}
import exceptions.{
  AppError,
  BadGoogleSignInAPIResponseCode,
  GoogleSignInAPIJsonParsingFailed,
  GoogleSignInVerificationFailed,
  GoogleSignInVerificationFailedWithEmailMismatch,
  NoSessionIdFoundInHeaders,
  NoValidUserForGivenSession,
  SessionIdParsingFailed,
  SessionNotFoundForUserWithEmail,
  UserWithGivenEmailNotFound,
  UserWithGivenIdNotFound
}
import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax._
import models._
import monix.eval.Task
import monix.execution.Scheduler
import play.api.{Environment, Logger}
import play.api.libs.circe.CirceBodyParsers
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Try

class HomeController(wsClient: AhcWSClient, dbRepo: DBRepo)(implicit
                                                            val environment: Environment,
                                                            val executionContext: Scheduler,
                                                            val controllerComponents: ControllerComponents)
    extends BaseController
    with CirceBodyParsers {

  private val logger = Logger("HomeController")

  def index(): Action[AnyContent] = Action.async(parse.anyContent) { _ =>
    val userId = UUID.randomUUID()
    dbRepo
      .createUser(User.strabo(userId))
      .map(_ => Ok("Dummy strabo user created! DB connection is successful!"))
      .runToFuture
  }

  def userExists(): Action[UserEmailAndAccessToken] = Action.async(circe.json[UserEmailAndAccessToken]) { req =>
    (for {
      verifiedUser <- verify(req.body)
      exists <- dbRepo.userExists(verifiedUser.email)
    } yield
      if (exists) Status(OK)
      else NotFound(HttpError(s"User with email: ${req.body.email} is not registered").asJson))
      .handleAppError()
      .runToFuture
  }

  def authToken(): Action[UserEmailAndAccessToken] = Action.async(circe.json[UserEmailAndAccessToken]) { req =>
    (for {
      verifiedUser <- verify(req.body)
      sessionId <- dbRepo.authToken(verifiedUser.email)
    } yield Ok(sessionId.asJson))
      .handleAppError()
      .runToFuture
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
          residenceCountry = req.body.residenceCountry,
          photoUrl = req.body.photoUrl,
          createdAt = LocalDateTime.now(ZoneId.of("UTC"))
        ))
      sessionId <- dbRepo.createAuthToken(userId)
    } yield Ok(sessionId.asJson))
      .handleAppError()
      .runToFuture
  }

  def user(): Action[AnyContent] = Action.async(parse.anyContent) { req =>
    withAuthToken(req) { authToken =>
      dbRepo
        .user(authToken.value)
        .map(UserInfo.fromUser)
    }.map(profile => Ok(profile.asJson))
      .handleAppError()
      .runToFuture
  }

  def logout(): Action[AnyContent] = Action.async(parse.anyContent) { req =>
    withAuthToken(req) { sessionId =>
      dbRepo.invalidateAuthToken(sessionId).map(_ => Ok(""))
    }.handleAppError().runToFuture
  }

  private def withAuthToken[A](req: Request[_])(f: AuthTokenValue => Task[A]): Task[A] =
    req.headers.get("Auth-Token") match {
      case Some(sessionIdStr) =>
        Try(UUID.fromString(sessionIdStr))
          .fold(_ => Task.raiseError(SessionIdParsingFailed(sessionIdStr)), id => Task.now(AuthTokenValue(id)))
          .flatMap(f)
      case None => Task.raiseError(NoSessionIdFoundInHeaders)
    }

  private def verify(info: UserEmailAndAccessToken): Task[GSignInEmail] =
    Task
      .fromFuture {
        wsClient
          .url(s"https://www.googleapis.com/oauth2/v1/tokeninfo")
          .addQueryStringParameters("access_token" -> info.accessToken)
          .get()
          .flatMap { res: WSResponse =>
            logger.info(s"Verifying $info, status: ${res.status} and response: ${res.json}")
            res.status match {
              case status if status >= 200 && status < 400 =>
                parser
                  .decode[GSignInEmail](res.body)
                  .fold(_ => Future.failed(GoogleSignInAPIJsonParsingFailed), Future.successful)
              case status if status == 400 => Future.failed(GoogleSignInVerificationFailed(info))
              case status                  => Future.failed(BadGoogleSignInAPIResponseCode(status))
            }
          }
      }
      .flatMap {
        case googleUser if googleUser.email == info.email => Task.now(googleUser)
        case googleUser =>
          Task.raiseError(GoogleSignInVerificationFailedWithEmailMismatch(info, googleUser.email))
      }

  implicit class TaskOps(task: Task[Result]) {

    //noinspection ScalaStyle
    def handleAppError(): Task[Result] =
      task
        .onErrorHandleWith { ex: Throwable =>
          logger.error("Error occurred while serving reqs", ex)
          Task.raiseError(ex)
        }
        .onErrorHandle {
          case error: AppError =>
            error match {
              case GoogleSignInAPIJsonParsingFailed =>
                InternalServerError(HttpError("Unknown error!").asJson)
              case BadGoogleSignInAPIResponseCode(_) =>
                InternalServerError(HttpError("Unknown error!").asJson)
              case GoogleSignInVerificationFailed(info) =>
                Unauthorized(HttpError(
                  s"Google sign-in verification failed (Possibly expired) for email: ${info.email}, access token: ${info.accessToken
                    .take(5)}...").asJson)

              case GoogleSignInVerificationFailedWithEmailMismatch(_, _) =>
                Unauthorized(HttpError(s"Google sign-in verification failed").asJson)
              case SessionIdParsingFailed(sessionIdStr) =>
                BadRequest(
                  HttpError(
                    s"Session id (sessionId): $sessionIdStr must be valid UUID"
                  ).asJson
                )
              case NoSessionIdFoundInHeaders =>
                BadRequest(
                  HttpError(
                    s"Session id (sessionId) is missing in request headers"
                  ).asJson
                )
              case UserWithGivenEmailNotFound(email) =>
                NotFound(
                  HttpError(
                    s"User with email: $email is not registered"
                  ).asJson
                )
              case UserWithGivenIdNotFound(id) =>
                NotFound(
                  HttpError(
                    "User involved in completing this request is not found"
                  ).asJson
                )
              case NoValidUserForGivenSession(tokenValue) =>
                Unauthorized(
                  HttpError(s"Invalid session id: ${tokenValue.value}").asJson
                )
              case SessionNotFoundForUserWithEmail(email) =>
                Unauthorized(
                  HttpError(
                    s"Session not found for user with email: $email"
                  ).asJson
                )
            }
          case _ => InternalServerError(HttpError("Unknown error!").asJson)
        }
  }

}
