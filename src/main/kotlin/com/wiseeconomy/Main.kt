package com.wiseeconomy

import com.wiseeconomy.db.*
import com.wiseeconomy.domain.*
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import com.wiseeconomy.db.*
import com.wiseeconomy.domain.*
import org.http4k.client.ApacheClient
import org.http4k.contract.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.filter.ResponseFilters
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto
import org.http4k.lens.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.*
import org.slf4j.LoggerFactory


val accessTokenHeader: String = "X-Access-Token"
val authTokenHeader: String = "X-Auth-Token"
val serverErrorBodyLens = Body.auto<ServerErrorBody<ErrorMessage>>().toLens()
val badRequestErrorBodyLens = Body.auto<ServerErrorBody<BadRequestErrorBody>>().toLens()

object HeaderLens {
    val accessToken = Header.nonEmptyString().required(accessTokenHeader)
    val authToken = Header.uuid().required(authTokenHeader)
}

object BodyLens {
    val email = Body.auto<ServerResponseBody<Email>>().toLens()
    val user = Body.auto<ServerResponseBody<User>>().toLens()
    val authToken = Body.auto<ServerResponseBody<AuthTokenValue>>().toLens()
}

fun main() {

    val logger = KotlinLogging.logger(LoggerFactory.getLogger("Mainkt"))

    val sqlLogger = object : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            logger.info { "SQL: ${context.expandArgs(transaction)}" }
        }
    }

    val config = Config.loadConfig

    logger.info { "Loaded application config: $config" }

    //val dataSource = AppDataSource.fromConfig(config.db)

    val dataSource = AppDataSource.fromDatabaseUrl(System.getenv("DATABASE_URL"))
    //Flyway.configure().dataSource(dataSource).load().migrate()

    val database: Database = Database.connect(dataSource)

    transaction(database) {
        addLogger(sqlLogger)
        val schema = Schema("strabo") // my_schema is the schema name.
        SchemaUtils.createSchema(schema)
        SchemaUtils.setSchema(schema)
        SchemaUtils.create(Users, AuthTokens)
    }

    val dao = DAOImpl(database)
    val controller = Controller(dao)

    val api: RoutingHttpHandler = contract {
        renderer = OpenApi3(ApiInfo("Strabox", "v1.0", "Strabo API backend"), Jackson)
        descriptionPath = "/swagger.json"
        routes += Routes(controller).all
    }

    val app = routes(api, Swagger.ui("/swagger.json"))


    val audit = ResponseFilters.ReportHttpTransaction { tx: HttpTransaction ->
        println("Call to ${tx.request.uri} returned ${tx.response.status} and took ${tx.duration.toMillis()} millis")
    }

    val globalErrorHandler = Filter { next ->
        {
            try {
                next(it)
            } catch (err: Throwable) {
                if (err !is AppError) {
                    err.printStackTrace()
                    Response(INTERNAL_SERVER_ERROR).with(serverErrorBodyLens of ServerErrorBody.withError(ErrorMessage("Unknown error!")))
                } else when (err) {
                    is UserNotFound -> Response(NOT_FOUND).with(serverErrorBodyLens of ServerErrorBody.withError(ErrorMessage(err.message
                            ?: "Unknown error!")))
                    is InvalidAccessToken ->
                        Response(UNAUTHORIZED).with(
                                serverErrorBodyLens of ServerErrorBody.unauthorized
                        )
                }
            }
        }

    }

    audit.then(globalErrorHandler)
            .then(ResponseFilters.GZip())
            .then(app)
            .asServer(Jetty(config.app.port)).start().block()
}

class Controller(val dao: DAO) {


    fun isRegisteredEmail(): HttpHandler = {
        val accessToken = HeaderLens.accessToken.extract(it)
        val email = Body.auto<Email>().toLens().extract(it)
        val verified = verify(accessToken, email)
        when {
            verified -> {
                val exists = dao.isRegisteredEmail(email)
                when {
                    exists -> Response(NO_CONTENT)
                    else -> Response(NOT_FOUND).with(serverErrorBodyLens of ServerErrorBody.notFound)
                }
            }
            else ->
                Response(UNAUTHORIZED).with(serverErrorBodyLens of ServerErrorBody.unauthorized)
        }
    }

    fun basicUserProfile(): HttpHandler = {
        val authToken = HeaderLens.authToken.extract(it)
        val user = dao.basicUserProfile(AuthTokenValue(authToken))
        if (user == null) Response(UNAUTHORIZED).with(serverErrorBodyLens of ServerErrorBody(ErrorMessage("Auth token is invalid")))
        else Response(OK).with(BodyLens.user of ServerResponseBody(user))
    }

    fun register(): HttpHandler = {
        val accessToken = HeaderLens.accessToken.extract(it)
        println("Register access token: $accessToken")
        val user = Body.auto<User>().toLens().extract(it)
        val verified = verify(accessToken, Email(user.email))
        when {
            verified -> {
                val exists = dao.isRegisteredEmail(Email(user.email))
                when {
                    exists -> Response(NO_CONTENT)
                    else -> {
                        dao.saveUser(user)
                        Response(CREATED)
                    }
                }
            }
            else ->
                Response(UNAUTHORIZED).with(serverErrorBodyLens of ServerErrorBody.unauthorized)
        }
    }

    fun authToken(): HttpHandler = {
        val accessToken = HeaderLens.accessToken.extract(it)
        val email = getEmail(accessToken)
        val isRegistered = dao.isRegisteredEmail(email)
        if (isRegistered) {
            when (val authToken = dao.getOrCreateAuthToken(email)) {
                is DAOAction.Companion.Created ->
                    Response(CREATED)
                            .with(BodyLens.authToken of ServerResponseBody(authToken.value))
                else -> Response(OK).with(BodyLens.authToken of ServerResponseBody(authToken.value))
            }
        } else Response(NOT_FOUND).with(serverErrorBodyLens of ServerErrorBody.notFound)
    }

    fun logout(): HttpHandler = {
        val authToken = HeaderLens.authToken.extract(it)
        if (dao.isValid(AuthTokenValue(authToken))) {
            dao.invalidate(AuthTokenValue(authToken))
            Response(NO_CONTENT)
        } else Response(UNAUTHORIZED).with(
                serverErrorBodyLens of ServerErrorBody(ErrorMessage("Auth token: $authToken is invalid"))
        )
    }

    private fun getEmail(accessToken: String): Email {
//        val client = ApacheClient()
//        val req = Request(GET, "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=$accessToken")
//        val resp = client(req)
//        if (resp.status.code >= 400) throw InvalidAccessToken(accessToken)
//        return Body.auto<Email>().toLens().extract(resp)
        return Email("$accessToken@gmail.com")
    }

    private fun verify(accessToken: String, email: Email): Boolean {
        return getEmail(accessToken) == email
    }
}

class Routes(ctrl: Controller) {


    val basePath: String = "api" / "v1"

    val isRegisteredEmail: ContractRoute = basePath / "isRegisteredEmail" meta {
        summary = "Check if email is registered."
        description = "Returns OK if email is registered or else NOT_FOUND."
        tags += Tag("User")
        headers += Header.nonEmptyString().required(accessTokenHeader)
        consumes += APPLICATION_JSON
        produces += APPLICATION_JSON
        receiving(Body.auto<Email>().toLens() to Email.dummyEmail)
        returning(
                NO_CONTENT to "Email is registered.")
        returning(
                BAD_REQUEST, badRequestErrorBodyLens to ServerErrorBody(BadRequestErrorBody.dummy), "Header or body is malformed or Access token is expired.")
        returning(
                UNAUTHORIZED, serverErrorBodyLens to ServerErrorBody.unauthorized, "Email retrieved from access token does not match given email in body.")
        returning(
                NOT_FOUND, serverErrorBodyLens to ServerErrorBody.notFound, "Email is not registered")
        returning(
                INTERNAL_SERVER_ERROR, serverErrorBodyLens to ServerErrorBody.internalServerError, "Server encountered an error while serving request.")
    } bindContract POST to ctrl.isRegisteredEmail()


    val basicUserProfile: ContractRoute = basePath / "basicUserProfile" meta {
        summary = "Get basic user profile."
        description = "Returns basic user profile of the user contains minimal information about the user."
        tags += Tag("User")
        headers += Header.uuid().required(authTokenHeader)
        produces += APPLICATION_JSON
        returning(
                OK, BodyLens.user to ServerResponseBody(User.dummyUser), "Basic user profile is returned.")
        returning(
                BAD_REQUEST, badRequestErrorBodyLens to ServerErrorBody(BadRequestErrorBody.dummy), "Header is malformed.")
        returning(
                UNAUTHORIZED, serverErrorBodyLens to ServerErrorBody.unauthorized, "Auth token is invalid")
        returning(
                NOT_FOUND, serverErrorBodyLens to ServerErrorBody.notFound, "User corresponding to Auth token is not found. Invalid auth token")
        returning(
                INTERNAL_SERVER_ERROR, serverErrorBodyLens to ServerErrorBody.internalServerError, "Server encountered an error while serving request.")
    } bindContract GET to ctrl.basicUserProfile()

    val register: ContractRoute = basePath / "register" meta {
        summary = "Register new user"
        description = "Register new user or return NO_CONTENT if user is registered already."
        tags += Tag("User")
        headers += Header.nonEmptyString().required(accessTokenHeader)
        consumes += APPLICATION_JSON
        produces += APPLICATION_JSON
        receiving(Body.auto<User>().toLens() to User.dummyUser)
        returning(
                CREATED to "User registration done.")
        returning(
                NO_CONTENT to "User is already registered.")
        returning(
                BAD_REQUEST, badRequestErrorBodyLens to ServerErrorBody(BadRequestErrorBody.dummy), "Header is malformed.")
        returning(
                UNAUTHORIZED, serverErrorBodyLens to ServerErrorBody.unauthorized, "Auth token is invalid.")
        returning(
                NOT_FOUND, serverErrorBodyLens to ServerErrorBody.notFound, "User corresponding to Auth token is not found.")
        returning(
                INTERNAL_SERVER_ERROR, serverErrorBodyLens to ServerErrorBody.internalServerError, "Server encountered an error while serving request.")
    } bindContract POST to ctrl.register()


    val authToken: ContractRoute = basePath / "authToken" meta {
        summary = "Get auth token."
        description = "Get auth token for the user to send further requests."
        tags += Tag("User")
        headers += Header.nonEmptyString().required(accessTokenHeader)
        produces += APPLICATION_JSON
        returning(
                OK, BodyLens.authToken to ServerResponseBody(AuthTokenValue.token), "Existing auth token returned")
        returning(
                CREATED, BodyLens.authToken to ServerResponseBody(AuthTokenValue.token), "Auth token is created and returned")
        returning(
                BAD_REQUEST, badRequestErrorBodyLens to ServerErrorBody(BadRequestErrorBody.dummy), "Header or body is malformed or Access token is expired.")
        returning(
                UNAUTHORIZED, serverErrorBodyLens to ServerErrorBody.unauthorized, "Auth token is invalid.")
        returning(
                NOT_FOUND, serverErrorBodyLens to ServerErrorBody.notFound, "User corresponding to Auth token is not found.")
        returning(
                INTERNAL_SERVER_ERROR, serverErrorBodyLens to ServerErrorBody.internalServerError, "Server encountered an error while serving request.")
    } bindContract POST to ctrl.authToken()

    val logout: ContractRoute = basePath / "logout" meta {
        summary = "Logout."
        description = "Logout invalidates auth token. New auth token has to be request for new requests."
        tags += Tag("User")
        headers += Header.nonEmptyString().required(authTokenHeader)
        produces += APPLICATION_JSON
        returning(
                NO_CONTENT to "User is logged out")
        returning(
                BAD_REQUEST, badRequestErrorBodyLens to ServerErrorBody(BadRequestErrorBody.dummy), "Auth token is malformed.")
        returning(
                UNAUTHORIZED, serverErrorBodyLens to ServerErrorBody.unauthorized, "Auth token is invalid.")
        returning(
                NOT_FOUND, serverErrorBodyLens to ServerErrorBody.notFound, "User corresponding to Auth token is not found.")
        returning(
                INTERNAL_SERVER_ERROR, serverErrorBodyLens to ServerErrorBody.internalServerError, "Server encountered an error while serving request.")
    } bindContract GET to ctrl.logout()

    val all = listOf(isRegisteredEmail, basicUserProfile, register, authToken, logout)
}