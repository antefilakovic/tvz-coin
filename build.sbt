name := "tvz-coin"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq("com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4",
  "com.typesafe.akka" %% "akka-remote" % "2.5.16")
