name := """strabo-backend"""

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.2"

libraryDependencies += ws
libraryDependencies += "com.opentable.components" % "otj-pg-embedded" % "0.13.3"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.14"
libraryDependencies += "io.getquill" %% "quill-core" % "3.5.2"
libraryDependencies += "io.getquill" %% "quill-jdbc-monix" % "3.5.2"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.12.3")

// test dependencies
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test"
