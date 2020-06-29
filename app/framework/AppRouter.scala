package framework

import controllers.{Assets, HomeController}
import play.api.routing.{Router, SimpleRouter}
import play.api.routing.sird._

class AppRouter(homeController: HomeController, assetsController: Assets) extends SimpleRouter {

  def routes: Router.Routes = {

    case GET(p"/") => homeController.index()

    case POST(p"/v1/userExists") => homeController.userExists()

    case POST(p"/v1/authToken") => homeController.authToken()

    case POST(p"/v1/register") => homeController.register()

    case GET(p"/v1/user") => homeController.user()

    case GET(p"/v1/logout") => homeController.logout()

    // static resources
    case GET(p"/assets/$file*") =>
      assetsController.versioned(path = "/public", file)

    case GET(p"/$file*") =>
      assetsController.versioned(path = "/public", file)
  }

}
