import sbt._

object Dependencies {
  private[this] val fastparse = "com.lihaoyi" %% "fastparse" % "1.0.0"
  private[this] val monix = "io.monix" %% "monix" % "3.0.0-RC1"
  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % "2.5.13"

  private[this] val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
  private[this] val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
  private[this] val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0" % Test
  private[this] val scalaMockTest = "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0"

  val runtime = Seq(fastparse, monix)
  val test = Seq(scalaTest, scalaCheck, scalaMock, scalaMockTest)

  val all: Seq[ModuleID] = runtime ++ test
}
