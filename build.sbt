name := "easyminer-rdf"

val basicSettings = Seq(
  organization := "com.github.KIZI.EasyMiner-Rdf",
  version := "1.0.0",
  scalaVersion := "2.12.3",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
)

lazy val root = project
  .in(file("."))
  .settings(basicSettings: _*)
  .aggregate(core/*, cli*/)

lazy val core = project
  .in(file("core"))

/*lazy val cli = project
  .in(file("cli"))
  .dependsOn(core)*/