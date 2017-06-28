name := "easyminer-rdf"

organization := "eu.easyminer"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions += "-feature"

libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
libraryDependencies += "org.apache.jena" % "jena-arq" % "3.1.1"