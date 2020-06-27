package models

import java.time.LocalDate
import java.util.UUID

case class GoogleSignedInUser(email: String, user_id: String)

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
