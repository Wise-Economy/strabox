name := "strabox"

version := "0.1"

scalaVersion := "2.13.3"

mainClass := Some("com.wiseeconomy.Main")

enablePlugins(JavaAppPackaging)

addCompilerPlugin(
  ("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)
)

addCompilerPlugin(
  "com.olegpy" %% "better-monadic-for" % "0.3.1"
)

scalacOptions := Seq(
//  "-feature",
//  "-deprecation",
  "-explaintypes",
//  "-unchecked",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:existentials",
//  "-Xfatal-warnings",
  "-Xlint:-infer-any,_",
  "-Ywarn-value-discard",
  "-Ywarn-numeric-widen",
  "-Ywarn-extra-implicit",
  "-Ywarn-unused:_"
) ++ (if (isSnapshot.value) Seq.empty else Seq("-opt:l:inline"))

val doobieVersion         = "0.8.8"
val zioVersion            = "1.0.0-RC21-2"
val zioInteropCatsVersion = "2.1.3.0-RC16"
val http4sVersion         = "0.21.6"
val circeVersion          = "0.12.3"
val pureconfigVersion     = "0.13.0"
val flywayCoreVersion     = "6.5.0"
val zioLoggingVersion     = "0.3.2"
val postgresDriverVersion = "42.2.14"
val quillJDBCVersion      = "3.5.2"

libraryDependencies ++= Seq(
  "io.getquill"           %% "quill-jdbc"          % quillJDBCVersion,
  "org.tpolecat"          %% "doobie-scalatest"    % doobieVersion % "test",
  "dev.zio"               %% "zio"                 % zioVersion,
  "dev.zio"               %% "zio-interop-cats"    % zioInteropCatsVersion,
  "dev.zio"               %% "zio-logging"         % zioLoggingVersion,
  "dev.zio"               %% "zio-logging-slf4j"   % zioLoggingVersion,
  "org.http4s"            %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"            %% "http4s-blaze-client" % http4sVersion,
  "org.http4s"            %% "http4s-circe"        % http4sVersion,
  "org.http4s"            %% "http4s-dsl"          % http4sVersion,
  "io.circe"              %% "circe-core"          % circeVersion,
  "io.circe"              %% "circe-generic"       % circeVersion,
  "io.circe"              %% "circe-parser"        % circeVersion,
  "com.github.pureconfig" %% "pureconfig"          % pureconfigVersion,
  "org.flywaydb"          % "flyway-core"          % flywayCoreVersion,
  "org.postgresql"        % "postgresql"           % postgresDriverVersion
)
