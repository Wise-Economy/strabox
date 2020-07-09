package com.wiseeconomy.domain

import java.time.LocalDate
import java.util.*

data class AuthTokenValue(val token: UUID) {
    companion object {
        val token = AuthTokenValue(UUID.randomUUID())
    }
}

data class Email(val email: String) {
    companion object {
        val dummyEmail = Email("foobar@foobarmail.com")
    }
}

data class ServerResponseBody<T>(val data: T)

data class ErrorMessage(val message: String)

data class ServerErrorBody<T>(val error: T) {
    companion object {
        fun <T> withError(value: T): ServerErrorBody<T> = ServerErrorBody(value)
        val unauthorized = ServerErrorBody(ErrorMessage("Unauthorized"))
        val notFound = ServerErrorBody(ErrorMessage("Not found"))
        val internalServerError = ServerErrorBody(ErrorMessage("Internal server error"))
    }
}

data class Param(
        val name: String,
        val type: String,
        val datatype: String,
        val required: Boolean,
        val reason: String
)

data class BadRequestErrorBody(
        val message: String,
        val params: List<Param>
) {
    companion object {
        val dummy = BadRequestErrorBody(
                message = "Missing/invalid parameters",
                params = listOf<Param>(
                        Param(
                                name = "X-Auth-Token",
                                type = "header",
                                datatype = "string",
                                required = true,
                                reason = "Invalid"
                        )
                )
        )
    }
}

data class User(
        val name: String,
        val email: String,
        val dob: LocalDate,
        val residenceCountry: String,
        val phoneCountryCode: String,
        val phoneNumber: String,
        val photoUrl: String
) {
    companion object {
        val dummyUser = User(
                name = "Foobar",
                email = "foobar@gmail.com",
                dob = LocalDate.now(),
                residenceCountry = "India",
                phoneCountryCode = "+91",
                phoneNumber = "1234567890",
                photoUrl = "http://www.lolcats.com/lolcat25.png"
        )
    }
}