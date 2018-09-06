name := "tvz-coin"

version := "0.2"

scalaVersion := "2.12.6"

enablePlugins(JavaAppPackaging)

dockerExposedPorts := Seq(9000, 2552, 5005)

libraryDependencies ++= Seq("com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4",
  "com.typesafe.akka" %% "akka-remote" % "2.5.16",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.60")

mainClass := Some("dev.afilakovic.TvzCoinApp")
