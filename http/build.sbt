name := "http"

organization := "com.github.propi.rdfrules"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

resolvers ++= Seq("jitpack" at "https://jitpack.io")

libraryDependencies += organization.value %% "core" % version.value
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.1"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.1"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11"
libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.4"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"