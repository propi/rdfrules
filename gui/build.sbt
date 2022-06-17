name := "gui"

organization := "com.github.propi.rdfrules"

version := "1.0.0"

scalaVersion := "2.13.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-P:scalajs:sjsDefinedByDefault"

scalaJSUseMainModuleInitializer := true

//jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

//scalaJSLinkerConfig ~= { _.withOptimizer(false) }

//libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
libraryDependencies += "com.thoughtworks.binding" %%% "binding" % "12.1.0"

scalacOptions ++= {
  import Ordering.Implicits._
  if (VersionNumber(scalaVersion.value).numbers >= Seq(2L, 13L)) {
    Seq("-Ymacro-annotations")
  } else {
    Nil
  }
}

//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)