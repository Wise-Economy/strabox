package com.wiseeconomy.strabox.services

import com.wiseeconomy.strabox.domain.Email
import org.http4s.client.Client
import zio.{ Has, Task, ZIO }

package object verifier {
  type HttpClient     = Has[Client[Task]]
  type AccessVerifier = Has[AccessVerifier.Service]

  def verify(
    accessToken: String,
    email: Email
  ): ZIO[AccessVerifier, Throwable, Boolean] =
    ZIO.accessM(_.get.verify(accessToken, email))
}
