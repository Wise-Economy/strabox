package com.wiseeconomy.strabox.services.db

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import doobie._
import doobie.postgres.implicits._
import doobie.implicits.javatime._
import doobie.implicits._
import zio.interop.catz._
import cats.effect.Blocker
import com.wiseeconomy.strabox.config.{ DBConfig, DbConfigProvider }
import com.wiseeconomy.strabox.db.DBAccess
import com.wiseeconomy.strabox.domain.{ AuthToken, Email, User, UserId }
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{ Managed, Task, UIO, ZIO, ZLayer, ZManaged }

final class PGAccess(xa: Transactor[Task]) {

  private val pgAccess = new DBAccess.Service {

    override def getUser(userId: UserId): UIO[Option[User]] =
      sql"""SELECT
           | name,
           | email,
           | dob,
           | phoneCountryCode,
           | phoneNumber,
           | residenceCountry,
           | photoUrl
           |FROM STRABO.USERS u WHERE u.id = ${userId.id}""".stripMargin
        .query[User]
        .option
        .transact(xa)
        .orDie

    override def saveUser(user: User): UIO[UserId] = {
      val id = UUID.randomUUID()
      sql"""
           |INSERT INTO strabo.users (
           | id,
           | name,
           | email,
           | dob,
           | phoneCountryCode,
           | phoneNumber,
           | residenceCountry,
           | photoUrl,
           | createdAt) VALUES (
           | $id,
           | ${user.name},
           | ${user.email.value},
           | ${user.dob},
           | ${user.phoneCountryCode},
           | ${user.phoneNumber},
           | ${user.residenceCountry},
           | ${user.photoUrl},
           | ${LocalDateTime.now()}
           |)
           |""".stripMargin.update.run
        .transact(xa)
        .orDie
        .as(UserId(id))
    }

    override def getOrCreateAuthToken(email: Email): UIO[AuthToken] = {
      val query = for {
        userId <- sql"""
                       |SELECT userId FROM strabo.users WHERE email =${email.value};
                       |""".stripMargin.query[UserId].unique
        token  <- sql"""
                      |SELECT validTokens.token
                      |FROM   strabo.users users
                      |       INNER JOIN (SELECT *
                      |                   FROM   strabo.auth_tokens tokens
                      |                   WHERE  tokens.invalidatedAt IS NULL) validTokens
                      |               ON ( users.id = validTokens.userId )
                      |WHERE  u.email = ${email.value};
                      |""".stripMargin.query[UUID].option
        newToken <- token match {
                     case Some(validToken) => validToken.pure[ConnectionIO]
                     case None =>
                       sql"""
                            |INSERT INTO strabo.authTokens (
                            |  token,
                            |  userId,
                            |  createdAt
                            |) VALUES (
                            |  ${UUID.randomUUID()},
                            |  ${userId.id},
                            |  ${LocalDateTime.now()}
                            |)
                            |""".stripMargin.update
                         .withUniqueGeneratedKeys[UUID]("token")
                   }
      } yield newToken

      query
        .transact(xa)
        .orDie
        .map(AuthToken)
    }

    override def exists(email: Email): UIO[Boolean] =
      sql"""
           |SELECT users.email FROM strabo.users WHERE users.email = ${email.value};
           |""".stripMargin
        .query[String]
        .option
        .transact(xa)
        .orDie
        .map(_.fold(false)(_ => true))

    override def isValid(token: AuthToken): UIO[Boolean] =
      sql"""
           |SELECT tokens.token FROM strabo.authTokens tokens
           |WHERE tokens.token = ${token.token} AND
           |tokens.invalidatedAt is NULL;
           |""".stripMargin
        .query[UUID]
        .option
        .transact(xa)
        .orDie
        .map(_.fold(false)(_ => true))

    override def getUserId(token: AuthToken): UIO[Option[UserId]] =
      sql"""
           |SELECT tokens.userId FROM strabo.authTokens tokens
           |WHERE tokens.token = ${token.token};
           |""".stripMargin
        .query[UUID]
        .option
        .transact(xa)
        .orDie
        .map(_.map(UserId))

    override def invalidate(token: AuthToken): UIO[Unit] =
      sql"""
           |UPDATE strabo.authTokens tokens SET tokens.invalidatedAt = ${LocalDateTime
             .now()}
           |WHERE tokens.token = ${token.token};
           |""".stripMargin.update.run
        .transact(xa)
        .orDie
        .unit
  }
}

object PGAccess {

  def live: ZLayer[
    Blocking with DbConfigProvider,
    Throwable,
    DBAccess
  ] = {
    def initDb(cfg: DBConfig): Task[Unit] =
      Task {
        Flyway
          .configure()
          .dataSource(cfg.url, cfg.user, cfg.password)
          .load()
          .migrate()
      }.unit

    def mkTransactor(
      cfg: DBConfig
    ): ZManaged[Blocking, Throwable, HikariTransactor[Task]] =
      ZIO.runtime[Blocking].toManaged_.flatMap { implicit rt =>
        for {
          transactEC <- Managed.succeed(
                         rt.environment
                           .get[Blocking.Service]
                           .blockingExecutor
                           .asEC
                       )
          connectEC = rt.platform.executor.asEC
          transactor <- HikariTransactor
                         .newHikariTransactor[Task](
                           cfg.driver,
                           cfg.url,
                           cfg.user,
                           cfg.password,
                           connectEC,
                           Blocker.liftExecutionContext(transactEC)
                         )
                         .toManaged
        } yield transactor
      }

    ZLayer.fromManaged {
      for {
        cfg        <- ZIO.service[DBConfig].toManaged_
        _          <- initDb(cfg).toManaged_
        transactor <- mkTransactor(cfg)
      } yield new PGAccess(transactor).pgAccess
    }
  }

}
