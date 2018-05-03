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
  .aggregate(core, java)

lazy val core = project
  .in(file("core"))

lazy val java = project
  .in(file("java"))
  .dependsOn(core)