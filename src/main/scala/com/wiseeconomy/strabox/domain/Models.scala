package com.wiseeconomy.strabox.domain

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

case class UserId(id: UUID)     extends AnyVal
case class Email(value: String) extends AnyVal

final case class User(
  name: String,
  email: Email,
  dob: LocalDate,
  phoneCountryCode: String,
  phoneNumber: String,
  residenceCountry: String,
  photoUrl: String)

case class AuthToken(token: UUID)

case class UserRow(
  id: UserId,
  payload: User,
  createdAt: LocalDateTime)

case class AuthTokenRow(
  token: AuthToken,
  userId: UserId,
  createdAt: LocalDateTime,
  invalidatedAt: LocalDateTime)
