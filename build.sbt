val akkaVersion = "2.4.20"
val amqpClientVersion = "4.1.1"
val playJsonVersion = "2.7.4"
val ficusVersion = "1.4.7"
val slf4jApiVersion = "1.7.25"
val logbackClassicVersion = "1.2.3"

lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "objektwerks",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.12",
  javaOptions in compile += "-Xss1m -Xmx2g",
  javaOptions in run += "-Xss1m -Xmx2g",
  scalacOptions ++= Seq(
    "-language:postfixOps",
    "-language:reflectiveCalls",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-feature",
    "-Ywarn-dead-code",
    "-unchecked",
    "-deprecation",
    "-Xfatal-warnings",
    "-Xlint:missing-interpolator",
    "-Xlint"
  ),
  fork in run := true
)

lazy val integrationTestSettings = Defaults.itSettings ++ Seq(
  compile in IntegrationTest <<= (compile in IntegrationTest) triggeredBy (compile in Test),
  parallelExecution in IntegrationTest := false,
  fork in IntegrationTest := true
)

lazy val testDependencies = {
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion % "provided, test",
    "com.typesafe.akka" %% "akka-remote" % akkaVersion % "provided, test",
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion % "provided, test",
    "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion % "provided, test",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % "provided, test",
    "com.rabbitmq" % "amqp-client" % amqpClientVersion % "provided",
    "com.iheart" %% "ficus" % ficusVersion % "provided",
    "com.typesafe.play" %% "play-json" % playJsonVersion % "provided",
    "org.slf4j" % "slf4j-api" % slf4jApiVersion % "test, it",
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion % "test, it",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test, it"
  )
}

lazy val clusterDependencies = {
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion % "provided",
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion % "provided",
    "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion % "provided"
  )
}

lazy val akkaDependencies = {
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.rabbitmq" % "amqp-client" % amqpClientVersion,
    "com.iheart" %% "ficus" % ficusVersion,
    "com.typesafe.play" %% "play-json" % playJsonVersion,
    "com.esotericsoftware.kryo" % "kryo" % "2.24.0",
    "tv.cntt" %% "chill-akka" % "1.1",
    "io.kamon" % "sigar-loader" % "1.6.6",
    "org.slf4j" % "slf4j-api" % slf4jApiVersion,
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion
  )
}

lazy val akkaDependencyOverrides = {
  Set(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  )
}

lazy val rootSettings = Seq (
  packagedArtifacts := Map.empty,
  publishLocal := {},
  publish := {}
)

lazy val clusterSettings = commonSettings ++ Seq(
  libraryDependencies ++= clusterDependencies
)

lazy val coreSettings = commonSettings ++ integrationTestSettings ++ Seq(
  libraryDependencies ++= testDependencies
)

lazy val seedNodeSettings = commonSettings ++ packAutoSettings ++ Seq(
  libraryDependencies ++=  akkaDependencies,
  dependencyOverrides ++= akkaDependencyOverrides,
  packCopyDependenciesUseSymbolicLinks := false,
  packJvmOpts := Map("seed-node" -> Seq("-server", "-Xms256m", "-Xmx1g"))
)

lazy val masterNodeSettings = commonSettings ++ packAutoSettings ++ Seq(
  libraryDependencies ++=  akkaDependencies,
  dependencyOverrides ++= akkaDependencyOverrides,
  packCopyDependenciesUseSymbolicLinks := false,
  packJvmOpts := Map("master-node" -> Seq("-server", "-Xss1m", "-Xms512m", "-Xmx2g"))
)

lazy val workerNodeSettings = commonSettings ++ packAutoSettings ++ Seq(
  libraryDependencies ++=  akkaDependencies,
  dependencyOverrides ++= akkaDependencyOverrides,
  packCopyDependenciesUseSymbolicLinks := false,
  packJvmOpts := Map("worker-node" -> Seq("-server", "-Xss1m", "-Xms1g", "-Xmx32g"))
)

lazy val akkaclusterx = (project in file(".")).
  settings(rootSettings: _*).
  aggregate(cluster, core, seednode, masternode, workernode)
lazy val cluster = project.
  settings(clusterSettings: _*)
lazy val core = project.
  settings(coreSettings: _*).
  configs(IntegrationTest)
lazy val seednode = project.
  settings(seedNodeSettings: _*).
  dependsOn(cluster)
lazy val masternode = project.
  settings(masterNodeSettings: _*).
  dependsOn(cluster, core)
lazy val workernode = project.
  settings(workerNodeSettings: _*).
  dependsOn(cluster, core)