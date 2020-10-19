name := "http"

organization := "com.github.propi.rdfrules"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

resolvers ++= Seq("jitpack" at "https://jitpack.io")

val akkaV = "2.6.9"
val akkaHttpV = "10.2.1"

libraryDependencies += organization.value %% "core" % version.value
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpV
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV
libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.4"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"