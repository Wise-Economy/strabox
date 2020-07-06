package com.wiseeconomy.strabox

import com.wiseeconomy.strabox.services.db.DBAccess
import com.wiseeconomy.strabox.domain.{AuthToken, Email, User, UserId}
import zio.{Has, URIO, ZIO}

package object db {
  type DBAccess = Has[DBAccess.Service]

  def getUser(id: UserId): URIO[DBAccess, Option[User]] =
    ZIO.accessM(_.get.getUser(id))

  def saveUser(user: User): URIO[DBAccess, UserId] =
    ZIO.accessM(_.get.saveUser(user))

  def getOrCreateAuthToken(email: Email): URIO[DBAccess, AuthToken] =
    ZIO.accessM(_.get.getOrCreateAuthToken(email))

  def exists(email: Email): URIO[DBAccess, Boolean] =
    ZIO.accessM(_.get.exists(email))

  def isValid(token: AuthToken): URIO[DBAccess, Boolean] =
    ZIO.accessM(_.get.isValid(token))

  def getUserId(token: AuthToken): URIO[DBAccess, Option[UserId]] =
    ZIO.accessM(_.get.getUserId(token))

  def invalidate(token: AuthToken): URIO[DBAccess, Unit] =
    ZIO.accessM(_.get.invalidate(token))
}
