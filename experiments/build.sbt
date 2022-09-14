name := "experiments"

organization := "com.github.propi.rdfrules"

version := "1.6.1"

scalaVersion := "2.13.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

resolvers ++= Seq("jitpack" at "https://jitpack.io")

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.36"

//fork in run := true
//javaOptions in run += "-agentlib:hprof=cpu=samples,depth=8"