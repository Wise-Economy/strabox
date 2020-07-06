package com.wiseeconomy.strabox.services.db

import com.wiseeconomy.strabox.domain.{AuthToken, Email, User, UserId}
import zio.UIO

object DBAccess extends Serializable {

  trait Service extends Serializable {

    def getUser(id: UserId): UIO[Option[User]]

    def saveUser(user: User): UIO[UserId]

    def getOrCreateAuthToken(email: Email): UIO[AuthToken]

    def exists(email: Email): UIO[Boolean]

    def isValid(token: AuthToken): UIO[Boolean]

    def getUserId(token: AuthToken): UIO[Option[UserId]]

    def invalidate(token: AuthToken): UIO[Unit]
  }

}