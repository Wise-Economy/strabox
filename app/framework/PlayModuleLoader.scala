package framework

import play.api.{Application, ApplicationLoader}

class PlayModuleLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = new MainModule(context).application
}
