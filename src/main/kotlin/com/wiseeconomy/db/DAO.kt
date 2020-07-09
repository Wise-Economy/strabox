package com.wiseeconomy.db

import com.wiseeconomy.domain.AuthTokenValue
import com.wiseeconomy.domain.Email
import com.wiseeconomy.domain.User
import com.wiseeconomy.domain.UserNotFound
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

sealed class DAOAction<T>(open val value: T) {
    companion object {
        data class Fetched<T>(override val value: T) : DAOAction<T>(value)
        data class Created<T>(override val value: T) : DAOAction<T>(value)
    }
}

interface DAO {
    fun isRegisteredEmail(email: Email): Boolean
    fun basicUserProfile(token: AuthTokenValue): User?
    fun saveUser(user: User): Unit
    fun getOrCreateAuthToken(email: Email): DAOAction<AuthTokenValue>
    fun invalidate(token: AuthTokenValue): Unit
    fun isValid(token: AuthTokenValue): Boolean
}

class DAOImpl(val database: Database) : DAO {

    override fun isRegisteredEmail(email: Email): Boolean {
        return transaction(database) {
            !Users.select { Users.email eq email.email }.empty()
        }
    }

    override fun basicUserProfile(token: AuthTokenValue): User? {
        return transaction(database) {
            val userId = transaction(database) {
                AuthTokens.slice(AuthTokens.userId)
                        .select { AuthTokens.id eq token.token }
                        .map { it[AuthTokens.userId] }
                        .singleOrNull()
            }
            userId?.let {
                transaction(database) {
                    Users.select { Users.id eq userId }.map {
                        User(
                                name = it[Users.name],
                                email = it[Users.email],
                                dob = it[Users.dob],
                                phoneCountryCode = it[Users.phoneCountryCode],
                                phoneNumber = it[Users.phoneNumber],
                                residenceCountry = it[Users.residenceCountry],
                                photoUrl = it[Users.photoUrl])
                    }.singleOrNull()
                }
            }
        }
    }

    override fun saveUser(user: User) {
        transaction(database) {
            Users.insert {
                it[id] = UUID.randomUUID()
                it[name] = user.name
                it[email] = user.email
                it[dob] = user.dob
                it[phoneCountryCode] = user.phoneCountryCode
                it[phoneNumber] = user.phoneNumber
                it[residenceCountry] = user.residenceCountry
                it[photoUrl] = user.photoUrl
                it[createdAt] = LocalDateTime.now()
            }
        }

    }

    override fun getOrCreateAuthToken(email: Email): DAOAction<AuthTokenValue> {
        return transaction(database) {

            val userId = transaction(database) {
                Users.slice(Users.id)
                        .select { Users.email eq email.email }
                        .map { it[Users.id] }
                        .singleOrNull()
            } ?: throw UserNotFound("User with email: ${email.email} not found")

            val token = transaction(database) {
                AuthTokens.slice(AuthTokens.id)
                        .select { AuthTokens.userId eq userId and AuthTokens.invalidatedAt.isNull() }
                        .map { it[AuthTokens.id] }
                        .singleOrNull()
            }

            if (token == null) {
                val newToken = UUID.randomUUID()
                AuthTokens.insert {
                    it[id] = newToken
                    it[AuthTokens.userId] = userId
                    it[createdAt] = LocalDateTime.now()
                    it[invalidatedAt] = null
                }
                DAOAction.Companion.Created(AuthTokenValue(newToken))
            } else DAOAction.Companion.Fetched(AuthTokenValue(token))
        }
    }

    override fun invalidate(token: AuthTokenValue) {
        transaction(database) {
            AuthTokens.update({ AuthTokens.id eq token.token }) {
                it[invalidatedAt] = LocalDateTime.now()
            }
        }
    }

    override fun isValid(token: AuthTokenValue): Boolean {
        return transaction(database) {
            !AuthTokens.select { AuthTokens.id eq token.token and AuthTokens.invalidatedAt.isNull() }.empty()
        }
    }
}