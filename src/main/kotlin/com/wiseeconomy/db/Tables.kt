package com.wiseeconomy.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object Users : Table("users") {
    val id = uuid("id")
    val name: Column<String> = varchar("name", 255)
    val email: Column<String> = varchar("email", 255).uniqueIndex()
    val dob: Column<LocalDate> = date("dob")
    val phoneCountryCode: Column<String> = varchar("phoneCountryCode", 5)
    val phoneNumber: Column<String> = varchar("phoneNumber", 20)
    val residenceCountry: Column<String> = varchar("residenceCountry", 255)
    val photoUrl: Column<String> = varchar("photoUrl", 255)
    val createdAt: Column<LocalDateTime> = datetime("createdAt")
    override val primaryKey = PrimaryKey(id)
}

object AuthTokens : Table("authTokens") {
    val id = uuid("id")
    val userId = uuid("userId") references Users.id
    val createdAt: Column<LocalDateTime> = datetime("createdAt")
    val invalidatedAt: Column<LocalDateTime?> = datetime("invalidatedAt").nullable()
    override val primaryKey = PrimaryKey(Users.id)
}

//class User(id: EntityID<UUID>) : UUIDEntity(id) {
//    companion object : UUIDEntityClass<User>(Users)
//    var name by Users.name
//    var email by Users.email
//    var dob by Users.dob
//    var phoneCountryCode by Users.phoneCountryCode
//    var phoneNumber by Users.phoneNumber
//    var residenceCountry by Users.residenceCountry
//    var photoUrl by Users.photoUrl
//    var createdAt by Users.createdAt
//    val authTokens by AuthToken referrersOn AuthTokens.userId
//}
//
//class AuthToken(id: EntityID<UUID>): UUIDEntity(id) {
//    companion object : UUIDEntityClass<AuthToken>(AuthTokens)
//    var user by User referencedOn Users.id
//    var createdAt by AuthTokens.createdAt
//    var invalidatedAt by AuthTokens.invalidatedAt
//}