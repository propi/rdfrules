name := "core"

organization := "com.github.propi.rdfrules"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

resolvers ++= Seq("jitpack" at "https://jitpack.io")

fork in Test := true

javaOptions in Test += "-javaagent:../tools/object-explorer.jar"

val jenaV = "3.4.0"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.apache.jena" % "jena-arq" % jenaV
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1"
libraryDependencies += "com.github.KIZI" % "easyminer-discretization" % "1.1.0"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.4"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6" % "test"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.7" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "com.github.shihyuho" % "memory-measurer" % "master-SNAPSHOT" % "test"