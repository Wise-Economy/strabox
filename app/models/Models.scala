package models

import java.time.LocalDate
import java.util.UUID

import database.User

case class HttpError(msg: String) extends AnyVal

case class UserRegistrationDetails(
    name: String,
    email: String,
    accessToken: String,
    dob: LocalDate,
    phoneCountryCode: String,
    phoneNumber: String,
    residenceCountry: String,
    photoUrl: String
)

case class UserInfo(
    name: String,
    email: String,
    dob: LocalDate,
    phoneCountryCode: String,
    phoneNumber: String,
    residenceCountryCode: String,
    photoUrl: String
)

object UserInfo {

  def fromUser(user: User): UserInfo = {
    UserInfo(
      name = user.name,
      email = user.email,
      dob = user.dob,
      phoneCountryCode = user.phoneCountryCode,
      phoneNumber = user.phoneNumber,
      residenceCountryCode = user.residenceCountry,
      photoUrl = user.photoUrl
    )
  }
}

case class UserId(id: UUID) extends AnyVal

case class AuthTokenValue(value: UUID) extends AnyVal

case class UserEmailAndAccessToken(email: String, accessToken: String)

case class GSignInEmail(email: String) extends AnyVal
