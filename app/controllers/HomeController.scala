package controllers

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import database.{DBRepo, User}
import exceptions.UserNotFound
import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax._
import models.{GSignInEmail, UserEmailAndAccessToken, UserRegistrationDetails}
import monix.eval.Task
import monix.execution.Scheduler
import play.api.Environment
import play.api.libs.circe.CirceBodyParsers
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc._

import scala.concurrent.Future

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
      sessionId <- dbRepo.getSession(verifiedUser.email)
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
    } yield Ok(sessionId.asJson)).onErrorHandle {
      case UserNotFound => NotFound
    }.runToFuture
  }

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
        case gsu @ GSignInEmail(email, _) if email == info.email => Task.now(gsu)
        case _                                                   => Task.raiseError(UserNotFound)
      }

}
