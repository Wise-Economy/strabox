package database

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import exceptions.{SessionNotFound, UserNotFound}
import io.getquill.{CompositeNamingStrategy2, PostgresEscape, PostgresMonixJdbcContext, SnakeCase}
import models.{SessionId, UserId}
import monix.eval.Task

import scala.concurrent.ExecutionContext

trait DBRepo {
  def createUser(user: User): Task[UserId]

  def createSession(userId: UserId): Task[SessionId]

  def session(email: String): Task[SessionId]

  def isUserRegistered(email: String): Task[Boolean]

  def sessionExists(sessionId: UUID): Task[Boolean]

  def userId(sessionId: SessionId): Task[UserId]

  def user(userId: UUID): Task[User]

  def invalidateSession(sessionId: SessionId): Task[Unit]
}

class DBRepoImpl(ctx: PostgresMonixJdbcContext[CompositeNamingStrategy2[SnakeCase.type, PostgresEscape.type]])(
    implicit ec: ExecutionContext,
) extends DBRepo {

  override def createUser(user: User): Task[UserId] = {

    import ctx._

    val insertedUserId = quote {
      querySchema[User]("strabo.user").insert(lift(User.strabo(UUID.randomUUID()))).returning(_.id)
    }
    run(insertedUserId).map(UserId)
  }

  override def createSession(userId: UserId): Task[SessionId] = {
    import ctx._
    val sessionId = quote {
      querySchema[UserSession]("strabo.session")
        .insert(
          lift(
            UserSession(
              id = UUID.randomUUID(),
              userId = userId.id,
              createdAt = LocalDateTime.now(ZoneId.of("UTC")),
              invalidatedAt = None
            )
          ))
        .returning(_.id)
    }
    run(sessionId).map(SessionId)
  }

  override def isUserRegistered(email: String): Task[Boolean] = {
    import ctx._
    val userFound = quote {
      querySchema[User]("strabo.user").filter(_.email == lift(email)).nonEmpty
    }
    run(userFound)
  }

  override def sessionExists(sessionId: UUID): Task[Boolean] = {
    import ctx._
    val sessionFound = quote {
      querySchema[UserSession]("strabo.session").filter(_.id == lift(sessionId)).nonEmpty
    }
    run(sessionFound)
  }

  override def session(email: String): Task[SessionId] = {
    import ctx._
    val validSession = quote {
      querySchema[User]("strabo.user")
        .filter(_.email == lift(email))
        .join(querySchema[UserSession]("strabo.session"))
        .on(_.id == _.userId)
        .map(_._2)
        .filter(session => session.invalidatedAt.isEmpty)
        .map(_.id)
        .take(1)
    }
    run(validSession).flatMap {
      case Nil     => Task.raiseError(SessionNotFound)
      case id :: _ => Task.now(SessionId(id))
    }
  }

  override def userId(sessionId: SessionId): Task[UserId] = {
    import ctx._
    val userWithSession = quote {
      querySchema[UserSession]("strabo.session")
        .filter(_.id == lift(sessionId.id))
        .take(1)
        .map(_.userId)
    }
    run(userWithSession).flatMap {
      case Nil       => Task.raiseError(UserNotFound)
      case uuid :: _ => Task.now(UserId(uuid))
    }
  }

  override def user(userId: UUID): Task[User] = {
    import ctx._
    val userQuery = quote {
      querySchema[User]("strabo.user")
        .filter(_.id == lift(userId))
        .take(1)
    }
    run(userQuery).flatMap {
      case Nil       => Task.raiseError(UserNotFound)
      case user :: _ => Task.now(user)
    }
  }

  override def invalidateSession(sessionId: SessionId): Task[Unit] = {
    import ctx._
    val now = LocalDateTime.now(ZoneId.of("UTC"))
    val invalidateQuery = quote {
      querySchema[UserSession]("strabo.session")
        .filter(_.id == lift(sessionId.id))
        .update(_.invalidatedAt -> lift(Some(now): Option[LocalDateTime]))
    }
    run(invalidateQuery).flatMap(_ => Task.unit)
  }
}
