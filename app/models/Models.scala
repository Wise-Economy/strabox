package models

import java.time.LocalDate
import java.util.UUID

import database.User

case class GSignInEmail(email: String)

case class UserId(id: UUID) extends AnyVal

case class SessionId(id: UUID) extends AnyVal

case class UserEmailAndAccessToken(email: String, accessToken: String)

case class UserRegistrationDetails(
    name: String,
    email: String,
    accessToken: String,
    dob: LocalDate,
    phoneCountryCode: String,
    phoneNumber: String,
    residenceCountryCode: String,
    photoUrl: String
)

case class UserProfile(
    name: String,
    email: String,
    dob: LocalDate,
    phoneCountryCode: String,
    phoneNumber: String,
    residenceCountryCode: String,
    photoUrl: String
)

object UserProfile {

  def fromUser(user: User): UserProfile = {
    UserProfile(
      name = user.name,
      email = user.email,
      dob = user.dob,
      phoneCountryCode = user.phoneCountryCode,
      phoneNumber = user.phoneNumber,
      residenceCountryCode = user.residenceCountryCode,
      photoUrl = user.photoUrl
    )
  }
}
