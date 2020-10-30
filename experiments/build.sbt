name := "experiments"

organization := "com.github.propi.rdfrules"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

resolvers ++= Seq("jitpack" at "https://jitpack.io")

libraryDependencies += organization.value %% "java" % version.value
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"

//fork in run := true
//javaOptions in run += "-agentlib:hprof=cpu=samples,depth=8"