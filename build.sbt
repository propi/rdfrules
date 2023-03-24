name := "rdfrules"

val basicSettings = Seq(
  organization := "com.github.propi.rdfrules",
  version := "1.7.2",
  scalaVersion := "2.13.8",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
)

lazy val root = project
  .in(file("."))
  .settings(basicSettings: _*)
  .aggregate(core, http, gui, experiments, experimentsAmie2, experimentsAmie3, experimentsKgc)

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
  .settings(basicSettings: _*)
  .settings(packMain := Map("main" -> "com.github.propi.rdfrules.experiments.RdfRulesExperiments"))
  .settings(libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.36")
  .enablePlugins(PackPlugin)
  .dependsOn(core)

lazy val experimentsAmie2 = project
  .in(file("experiments_amie2"))
  .settings(basicSettings: _*)
  .settings(packMain := Map("main" -> "com.github.propi.rdfrules.experiments.OriginalAmieComparison"))
  .enablePlugins(PackPlugin)
  .dependsOn(experiments)

lazy val experimentsAmie3 = project
  .in(file("experiments_amie3"))
  .settings(basicSettings: _*)
  .settings(packMain := Map("main" -> "com.github.propi.rdfrules.experiments.Amie3Comparison"))
  .enablePlugins(PackPlugin)
  .dependsOn(experiments)

lazy val experimentsKgc = project
  .in(file("experiments_kgc"))
  .settings(basicSettings: _*)
  .settings(packMain := Map("main" -> "com.github.propi.rdfrules.experiments.RdfRulesKgc"))
  .enablePlugins(PackPlugin)
  .dependsOn(experiments)