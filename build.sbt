seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "taskmaster"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0"

libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0"

