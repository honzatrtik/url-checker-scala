ThisBuild / version := "0.2"
ThisBuild / scalaVersion := "2.13.2"

val http4sVersion = "0.21.4"
lazy val root = (project in file("."))
  .settings(
    name := "url-checker",
    libraryDependencies := Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.typelevel" %% "cats-effect" % "2.1.3",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",
    ),
    bintrayRepository := "maven",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  )