package com.wiseeconomy.strabox.services.verifier

import com.wiseeconomy.strabox.domain.Email
import zio._

object AccessVerifier {

  trait Service {

    def verify(
      accessToken: String,
      email: Email
    ): Task[Boolean]
  }

}
