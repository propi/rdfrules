name := "core"

organization := "com.github.KIZI.EasyMiner-Rdf"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

val akkaV = "2.5.6"
val jenaV = "3.4.0"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7"
libraryDependencies += "org.apache.jena" % "jena-arq" % jenaV
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaV
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1"
libraryDependencies += "com.github.KIZI" %% "easyminer-discretization" % "1.1.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

//libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
//libraryDependencies += "com.github.shihyuho" % "memory-measurer" % "master-SNAPSHOT"