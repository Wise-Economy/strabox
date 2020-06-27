package framework

import java.net.URI

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import controllers.{AssetsComponents, HomeController}
import database.DBRepoImpl
import io.getquill._
import io.getquill.context.monix.Runner
import monix.eval.Task
import monix.execution.Scheduler
import play.api._
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.{ControllerComponents, EssentialFilter}
import play.filters.cors.{CORSConfig, CORSFilter}
import play.filters.gzip.{GzipFilter, GzipFilterConfig}
import play.filters.hosts.{AllowedHostsConfig, AllowedHostsFilter}

class MainModule(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents {

  val logger = Logger("MainModule")
  val corsFilter = new CORSFilter(CORSConfig.fromConfiguration(configuration))
  val gzipFilter = new GzipFilter(GzipFilterConfig.fromConfiguration(configuration))

  val allowedHostsFilter = AllowedHostsFilter(
    AllowedHostsConfig.fromConfiguration(configuration),
    httpErrorHandler
  )

  def httpFilters: Seq[EssentialFilter] = Seq(
    allowedHostsFilter,
    corsFilter,
    gzipFilter
  )

  override implicit lazy val executionContext: Scheduler = monix.execution.Scheduler.Implicits.global
  implicit val implicitEnvironment: Environment = environment
  implicit val implicitControllerComponents: ControllerComponents = controllerComponents

  val wsClient = AhcWSClient()

  val dbUri = new URI(System.getenv("DATABASE_URL"))
  val username: String = dbUri.getUserInfo.split(":")(0)
  val password: String = dbUri.getUserInfo.split(":")(1)
  val host: String = dbUri.getHost
  val port: Int = dbUri.getPort
  val databaseName: String = dbUri.getPath

  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setDatabaseName(databaseName)
  pgDataSource.setUser(username)
  pgDataSource.setPassword(password)
  pgDataSource.setServerNames(Array(host))
  pgDataSource.setPortNumbers(Array(port))
  logger.info(s"DB: $databaseName, User: $username, Pass: $password, Host: $host, port: $port")

  val config = new HikariConfig()
  config.setDataSource(pgDataSource)

  val ctx: PostgresMonixJdbcContext[CompositeNamingStrategy2[SnakeCase.type, PostgresEscape.type]] =
    new PostgresMonixJdbcContext(NamingStrategy(SnakeCase, PostgresEscape),
                                 new HikariDataSource(config),
                                 Runner.default)

  val dbRepo = new DBRepoImpl(ctx)

  val homeController = new HomeController(wsClient, dbRepo)

  override val router = new AppRouter(homeController, assets)

  applicationLifecycle.addStopHook { () =>
    for {
      _ <- Task(wsClient.close()).runToFuture
    } yield ()
  }
}
