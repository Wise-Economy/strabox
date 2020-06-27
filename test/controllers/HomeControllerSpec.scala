package controllers

import akka.stream.Materializer
import akka.util.Timeout
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, route}
import providers.PlayPerSuiteProvider

import scala.concurrent.duration._

class HomeControllerSpec extends PlaySpec with PlayPerSuiteProvider {

  "HomeController" must {

    "Index route is GET not POST" in {
      implicit val timeout: Timeout = Timeout(10.seconds)
      val req = FakeRequest(POST, "/")
        .withBody[String]("")
        .withHeaders("Content-type" -> "application/json; utf-8")
      val result = route(app, req)
      result.map(status) mustBe Some(NOT_FOUND)
    }

  }

  implicit val environment: Environment = context.environment
  implicit val implicitControllerComponents: ControllerComponents = components.controllerComponents
  implicit val mat: Materializer = components.materializer

  val controller: HomeController = components.homeController

}
