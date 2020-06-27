package providers

import framework.MainModule
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.ApplicationLoader.Context
import play.api.http.{HeaderNames, Status}
import play.api.test.ResultExtractors
import play.api.{ApplicationLoader, Environment, Mode}

trait PlayPerSuiteProvider extends OneAppPerSuiteWithComponents with HeaderNames with Status with ResultExtractors {
  self: PlaySpec =>

  override lazy val context: ApplicationLoader.Context =
    Context.create(Environment.simple(mode = Mode.Test))

  override def components: MainModule = new MainModule(context)
}
