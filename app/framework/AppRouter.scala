package framework

import controllers.{Assets, HomeController}
import io.getquill.{PostgresJdbcContext, SnakeCase}
import play.api.routing.{Router, SimpleRouter}
import play.api.routing.sird._

class AppRouter(homeController: HomeController, assetsController: Assets) extends SimpleRouter {

  def routes: Router.Routes = {

    case GET(p"/") => homeController.index()

    case POST(p"/v1/isRegisteredUser") => homeController.isRegisteredUser()

    case POST(p"/v1/session") => homeController.session()

    case POST(p"/v1/register") => homeController.register()

    // static resources
    case GET(p"/assets/$file*") =>
      assetsController.versioned(path = "/public", file)

    case GET(p"/$file*") =>
      assetsController.versioned(path = "/public", file)
  }

}