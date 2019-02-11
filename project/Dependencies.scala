import sbt._

object WellcomeDependencies {
  val fixturesLibrary: Seq[ModuleID] = library(
    name = "fixtures",
    version = "1.0.0"
  )

  val jsonLibrary: Seq[ModuleID] = library(
    name = "json",
    version = "1.1.1"
  )

  val messagingLibrary: Seq[ModuleID] = library(
    name = "messaging",
    version = "1.1.2"
  )

  val monitoringLibrary: Seq[ModuleID] = library(
    name = "monitoring",
    version = "1.2.1"
  )

  val storageLibrary: Seq[ModuleID] = library(
    name = "storage",
    version = "3.2.1"
  )

  private def library(name: String, version: String): Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% name % version,
    "uk.ac.wellcome" %% name % version % "test" classifier "tests"
  )
}

object Dependencies {

  lazy val versions = new {
    val akka = "2.5.9"
    val akkaHttp = "10.1.5"
    val akkaStreamAlpakka = "0.20"
    val aws = "1.11.95"
    val apacheLogging = "2.8.2"
    val finatra = "18.11.0"
    val guice = "4.2.0"
    val logback = "1.1.8"
    val mockito = "1.9.5"
    val scalatest = "3.0.1"
    val junitInterface = "0.11"
    val elastic4s = "6.5.0"
    val circeVersion = "0.9.0"
    val scalaCheckVersion = "1.13.4"
    val scalaCheckShapelessVersion = "1.1.6"
    val scalaCsv = "1.3.5"
    val jaxbVersion = "2.2.11"
    val typesafe = "1.3.2"
    val wiremockVersion = "2.18.0"
    val apacheCommons = "2.6"
  }

  val apacheCommons = Seq(
    "commons-io" % "commons-io" % versions.apacheCommons % "test")

  // External Library dependency groups
  val akkaDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-stream" % versions.akka
  )

  val circeOpticsDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val swaggerDependencies = Seq(
    "com.jakehschwartz" %% "finatra-swagger" % versions.finatra
  )

  val loggingDependencies = Seq(
    "org.clapper" %% "grizzled-slf4j" % "1.3.2"
  )

  val guiceDependencies = Seq(
    "com.google.inject" % "guice" % versions.guice,
    "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test"
  )

  val sharedDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val scalacheckDependencies = Seq(
    "org.scalacheck" %% "scalacheck" % versions.scalaCheckVersion % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalaCheckShapelessVersion % "test"
  )

  val finatraDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "com.twitter" %% "finatra-http" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "finatra-http" % versions.finatra,
    "com.twitter" %% "finatra-httpclient" % versions.finatra,
    "com.twitter" %% "finatra-jackson" % versions.finatra % "test",
    "com.twitter" %% "finatra-jackson" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-app" % versions.finatra % "test",
    "com.twitter" %% "inject-core" % versions.finatra,
    "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-modules" % versions.finatra % "test",
    "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-server" % versions.finatra % "test"
  )

  val elasticsearchDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-core" % versions.apacheLogging,
    "org.apache.logging.log4j" % "log4j-api" % versions.apacheLogging,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
  )

  val testDependencies = Seq(
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "com.novocode" % "junit-interface" % versions.junitInterface % "test",
    "javax.xml.bind" % "jaxb-api" % versions.jaxbVersion % "test"
  )

  val wiremockDependencies = Seq(
    "com.github.tomakehurst" % "wiremock" % versions.wiremockVersion % "test"
  )

  // Internal Library dependency groups
  val commonDependencies =
    testDependencies ++
      loggingDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-actor" % versions.akka % "test",
      "com.typesafe.akka" %% "akka-stream" % versions.akka % "test"
    ) ++ apacheCommons ++ WellcomeDependencies.fixturesLibrary

  val commonDisplayDependencies = swaggerDependencies ++ guiceDependencies ++ scalacheckDependencies

  val commonElasticsearchDependencies: Seq[ModuleID] =
    elasticsearchDependencies ++
      circeOpticsDependencies ++
      guiceDependencies ++
      scalacheckDependencies ++
      WellcomeDependencies.fixturesLibrary

  val apiDependencies: Seq[ModuleID] =
    Seq(
      "com.typesafe.akka" %% "akka-actor" % versions.akka,
    ) ++
      finatraDependencies ++
      guiceDependencies ++
      WellcomeDependencies.fixturesLibrary

  val typesafeDependencies: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % versions.typesafe
  )

  val typesafeCoreDependencies: Seq[ModuleID] = typesafeDependencies ++ akkaDependencies

  val configMessagingDependencies: Seq[ModuleID] = typesafeDependencies ++ WellcomeDependencies.messagingLibrary

  val typesafeMonitoringDependencies: Seq[ModuleID] = typesafeDependencies ++ WellcomeDependencies.monitoringLibrary

  val typesafeStorageDependencies: Seq[ModuleID] = akkaDependencies ++ typesafeDependencies ++ WellcomeDependencies.storageLibrary

  val internalModelDependencies = Seq(
    "com.github.tototoshi" %% "scala-csv" % versions.scalaCsv
  ) ++ WellcomeDependencies.jsonLibrary

  // Application specific dependency groups

  val snapshotGeneratorDependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakka
  )

  val storageCommonDependencies: Seq[ModuleID] = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakka,
    "com.lightbend.akka" %% "akka-stream-alpakka-sns" % versions.akkaStreamAlpakka,
    "com.typesafe.akka" %% "akka-http" % versions.akkaHttp,
    "org.rogach" %% "scallop" % "3.1.3",
    "de.heikoseeberger" %% "akka-http-circe" % "1.21.1",
    "com.amazonaws" % "aws-java-sdk-cloudwatchmetrics" % versions.aws
  ) ++ akkaDependencies ++ typesafeDependencies ++ WellcomeDependencies.storageLibrary ++ WellcomeDependencies.jsonLibrary ++ WellcomeDependencies.monitoringLibrary

  val bagsApiDependencies: Seq[ModuleID] = circeOpticsDependencies
  val ingestsApiDependencies: Seq[ModuleID] = circeOpticsDependencies
}
