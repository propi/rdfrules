name := "core"

organization := "com.github.propi.rdfrules"

version := "1.8.0"

scalaVersion := "2.13.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", /*"-opt:inline",*/ "-encoding", "utf8")
//scalacOptions += "-Xlog-implicits"

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

resolvers ++= Seq("jitpack" at "https://jitpack.io")

fork in Test := true

javaOptions in Test += "-javaagent:../tools/object-explorer.jar"

val jenaV = "3.4.0"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.apache.jena" % "jena-arq" % jenaV
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
//libraryDependencies += "com.github.KIZI" %% "easyminer-discretization" % "1.3.0"
libraryDependencies += "com.github.KIZI" % "easyminer-discretization" % "822da73931"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"
//libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6" % "test"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.36" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % "test"
libraryDependencies += "com.github.shihyuho" % "memory-measurer" % "e0d995b618" % "test"
libraryDependencies += "it.unimi.dsi" % "fastutil" % "8.2.2"
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.18"
libraryDependencies += "com.github.jsqlparser" % "jsqlparser" % "3.0"