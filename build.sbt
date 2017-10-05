name := "easyminer-rdf"

organization := "eu.easyminer"

version := "1.0"

scalaVersion := "2.12.3"

scalacOptions += "-feature"
//scalacOptions += "-Xlog-implicits"

//resolvers ++= Seq("jitpack" at "https://jitpack.io")

val akkaV = "2.5.3"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"
libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
libraryDependencies += "org.apache.jena" % "jena-arq" % "3.1.1"
//libraryDependencies += "com.github.shihyuho" % "memory-measurer" % "master-SNAPSHOT"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaV
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1"