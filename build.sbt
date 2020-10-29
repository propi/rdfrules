name := "rdfrules"

val basicSettings = Seq(
  organization := "com.github.propi.rdfrules",
  version := "1.0.0",
  scalaVersion := "2.12.3",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
)

lazy val root = project
  .in(file("."))
  .settings(basicSettings: _*)
  .aggregate(core, http, gui, experiments)

lazy val core = project
  .in(file("core"))

lazy val http = project
  .in(file("http"))
  .settings(packMain := Map("main" -> "com.github.propi.rdfrules.http.Main"))
  .enablePlugins(PackPlugin)
  .dependsOn(core)

lazy val gui = project
  .in(file("gui"))
  .enablePlugins(ScalaJSPlugin)

lazy val experiments = project
  .in(file("experiments"))
  .settings(packMain := Map("main" -> "com.github.propi.rdfrules.experiments.OriginalAmieComparison"))
  .enablePlugins(PackPlugin)
  .dependsOn(core)