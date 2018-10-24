import Dependencies._

val settings: Seq[Setting[_]] = Seq(
  organization := "anonymous",
  scalaVersion := "2.12.6",
  version      := "0.1.0-SNAPSHOT",
  libraryDependencies ++= all
)

mainClass in (Compile, run) := Some("seuraajaa.Server")
mainClass in assembly := Some("seuraajaa.Server")

lazy val root = (project in file("."))
  .settings(settings)
  .settings(name := "seuraajaa")
  .settings(assemblyJarName in assembly := "seuraajaa.jar")
  .dependsOn(core, `functional-tests`)
  .aggregate(core, `functional-tests`)

lazy val core = (project in file("modules/core"))
  .settings(settings)
  .settings(name := "core")

lazy val benchmarks = (project in file("modules/benchmarks"))
  .settings(settings)
  .settings(name := "benchmarks")
  .settings(libraryDependencies += akkaStreams)
  .enablePlugins(JmhPlugin)
  .dependsOn(core % "compile->compile;test->test")

lazy val `functional-tests` = (project in file("modules/functional-tests"))
  .settings(settings)
  .settings(name := "functional-tests")
  .dependsOn(core % "compile->compile;test->test")