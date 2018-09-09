name := "tvz-coin"

version := "0.2"

scalaVersion := "2.12.6"

enablePlugins(JavaAppPackaging)

dockerExposedPorts := Seq(8080, 2552, 5005)

mappings in Universal += (sourceDirectory.value / "main" / "resources"/ "genesis-block.pub", "src/main/resources/genesis-block.pub")
mappings in Universal += (sourceDirectory.value / "main" / "resources"/ "genesis-block.sig", "src/main/resources/genesis-block.sig")
mappings in Universal += (sourceDirectory.value / "main" / "resources"/ "init.key", "init.key") //used for testing app with docker-compose so first node can load keys
mappings in Universal += (sourceDirectory.value / "main" / "resources"/ "init.pub", "init.pub") //used for testing app with docker-compose so first node can load keys

libraryDependencies ++= Seq("com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4",
  "com.typesafe.akka" %% "akka-remote" % "2.5.16",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.16",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.60",
  "io.circe" %% "circe-core" % "0.9.3",
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.4",
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",
  "ch.megard" %% "akka-http-cors" % "0.3.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value)

mainClass := Some("dev.afilakovic.TvzCoinApp")
