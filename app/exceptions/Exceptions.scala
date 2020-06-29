package exceptions

import models.{AuthTokenValue, UserEmailAndAccessToken, UserId}

sealed trait AppError extends Throwable {
  override def getMessage: String = ""
}

case object GoogleSignInAPIJsonParsingFailed extends AppError
case class BadGoogleSignInAPIResponseCode(code: Int) extends AppError
case class GoogleSignInVerificationFailed(info: UserEmailAndAccessToken) extends AppError
case class GoogleSignInVerificationFailedWithEmailMismatch(info: UserEmailAndAccessToken, emailFromAPI: String)
    extends AppError
case class SessionIdParsingFailed(sessionIdStr: String) extends AppError
case object NoSessionIdFoundInHeaders extends AppError
case class UserWithGivenEmailNotFound(email: String) extends AppError
case class UserWithGivenIdNotFound(id: UserId) extends AppError
case class NoValidUserForGivenSession(token: AuthTokenValue) extends AppError
case class SessionNotFoundForUserWithEmail(email: String) extends AppError
