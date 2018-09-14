name := "gui"

organization := "com.github.propi.rdfrules"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
scalacOptions += "-P:scalajs:sjsDefinedByDefault"

scalaJSUseMainModuleInitializer := true

jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

libraryDependencies += "com.thoughtworks.binding" %%% "dom" % "11.0.1"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)