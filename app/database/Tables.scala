package database

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

case class User(
    id: UUID,
    name: String,
    email: String,
    dob: LocalDate,
    phoneCountryCode: String,
    phoneNumber: String,
    residenceCountryCode: String,
    photoUrl: String,
    createdAt: LocalDateTime
)

object User {

  def strabo(id: UUID): User = User(
    id = id,
    name = "Strabo",
    email = "straboapp@gmail.com",
    dob = LocalDate.now(),
    phoneCountryCode = "+91",
    phoneNumber = "123456789",
    residenceCountryCode = "in",
    photoUrl = "http://example.com",
    createdAt = LocalDateTime.now()
  )
}

case class UserSession(
    id: UUID,
    userId: UUID,
    createdAt: LocalDateTime,
    invalidatedAt: Option[LocalDateTime]
)
