package database

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import exceptions.{NoValidUserForGivenSession, SessionNotFoundForUserWithEmail, UserWithGivenIdNotFound}
import io.getquill.{CompositeNamingStrategy2, PostgresEscape, PostgresMonixJdbcContext, SnakeCase}
import models.{AuthTokenValue, UserId}
import monix.eval.Task

import scala.concurrent.ExecutionContext

trait DBRepo {
  def createUser(user: User): Task[UserId]

  def createAuthToken(userId: UserId): Task[AuthTokenValue]

  def authToken(email: String): Task[AuthTokenValue]

  def userExists(email: String): Task[Boolean]

  def isValid(authToken: AuthTokenValue): Task[Boolean]

  def userId(authToken: AuthToken): Task[UserId]

  def user(userId: UUID): Task[User]

  def invalidateAuthToken(token: AuthTokenValue): Task[Unit]
}

class DBRepoImpl(val ctx: PostgresMonixJdbcContext[CompositeNamingStrategy2[SnakeCase.type, PostgresEscape.type]])(
    implicit ec: ExecutionContext,
) extends DBRepo {

  import ctx._

  implicit val userSchemaMeta = schemaMeta[User]("strabo.user")
  implicit val authTokenSchemaMeta = schemaMeta[AuthToken]("strabo.auth_token")

  override def createUser(user: User): Task[UserId] = {

    val insertedUserId = quote {
      query[User].insert(lift(user)).returning(_.id)
    }
    run(insertedUserId).map(UserId)
  }

  override def createAuthToken(userId: UserId): Task[AuthTokenValue] = {
    val sessionId = quote {
      query[AuthToken]
        .insert(
          lift(
            AuthToken(
              token = UUID.randomUUID(),
              userId = userId.id,
              createdAt = LocalDateTime.now(ZoneId.of("UTC")),
              invalidatedAt = None
            )
          ))
        .returning(_.token)
    }
    run(sessionId).map(AuthTokenValue(_))
  }

  override def userExists(email: String): Task[Boolean] = {
    val userFound = quote {
      query[User].filter(_.email == lift(email)).nonEmpty
    }
    run(userFound)
  }

  override def isValid(authToken: AuthTokenValue): Task[Boolean] = {
    val tokenFound = quote {
      query[AuthToken].filter(_.token == lift(authToken.value)).nonEmpty
    }
    run(tokenFound)
  }

  override def authToken(email: String): Task[AuthTokenValue] = {
    val validToken = quote {
      query[User]
        .filter(_.email == lift(email))
        .join(query[AuthToken])
        .on(_.id == _.userId)
        .map(_._2)
        .filter(session => session.invalidatedAt.isEmpty)
        .map(_.token)
        .take(1)
    }
    run(validToken).flatMap {
      case Nil     => Task.raiseError(SessionNotFoundForUserWithEmail(email))
      case id :: _ => Task.now(AuthTokenValue(id))
    }
  }

  override def userId(token: AuthToken): Task[UserId] = {
    val userWithSession = quote {
      query[AuthToken]
        .filter(_.token == lift(token.token))
        .take(1)
        .map(_.token)
    }
    run(userWithSession).flatMap {
      case Nil       => Task.raiseError(NoValidUserForGivenSession(AuthTokenValue(token.token)))
      case uuid :: _ => Task.now(UserId(uuid))
    }
  }

  override def user(userId: UUID): Task[User] = {
    val userQuery = quote {
      query[User]
        .filter(_.id == lift(userId))
        .take(1)
    }
    run(userQuery).flatMap {
      case Nil       => Task.raiseError(UserWithGivenIdNotFound(UserId(userId)))
      case user :: _ => Task.now(user)
    }
  }

  override def invalidateAuthToken(token: AuthTokenValue): Task[Unit] = {
    val now = LocalDateTime.now(ZoneId.of("UTC"))
    val invalidateQuery = quote {
      query[AuthToken]
        .filter(_.token == lift(token.value))
        .update(_.invalidatedAt -> lift(Some(now): Option[LocalDateTime]))
    }
    run(invalidateQuery).flatMap(_ => Task.unit)
  }
}
