name := "core"

organization := "eu.easyminer.rdf"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

val akkaV = "2.5.6"
val jenaV = "3.4.0"

//libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"
libraryDependencies += "org.apache.jena" % "jena-arq" % jenaV
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaV
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1"

//libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
//libraryDependencies += "com.github.shihyuho" % "memory-measurer" % "master-SNAPSHOT"