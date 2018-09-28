import WellcomeDependencies.versions
import sbt._

object WellcomeDependencies {
  private lazy val versions = new {
    val json = "1.0.0"
    val monitoring = "1.1.0"
    val storage = "2.3.0"
  }

  val jsonLibrary: Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% "json" % versions.json,
    "uk.ac.wellcome" %% "json" % versions.json % "test" classifier "tests"
  )

  val monitoringLibrary: Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% "monitoring" % versions.monitoring,
    "uk.ac.wellcome" %% "monitoring" % versions.monitoring % "test" classifier "tests"
  )

  val storageLibrary: Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% "storage" % versions.storage,
    "uk.ac.wellcome" %% "storage" % versions.storage % "test" classifier "tests"
  )
}

object Dependencies {

  lazy val versions = new {
    val akka = "2.5.9"
    val akkaStreamAlpakka = "0.20"
    val aws = "1.11.95"
    val apacheLogging = "2.8.2"
    val finatra = "18.4.0"
    val guice = "4.2.0"
    val logback = "1.1.8"
    val mockito = "1.9.5"
    val scalatest = "3.0.1"
    val junitInterface = "0.11"
    val elastic4s = "5.6.5"
    val scanamo = "1.0.0-M3"
    val circeVersion = "0.9.3"
    val scalaCheckVersion = "1.13.4"
    val scalaCheckShapelessVersion = "1.1.6"
    val scalaCsv = "1.3.5"
    val sierraStreamsSourceVersion = "0.4"
    val jaxbVersion = "2.2.11"
    val scalaGraphVersion = "1.12.5"
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

  val dynamoDependencies: Seq[ModuleID] = Seq(
    "com.amazonaws" % "aws-java-sdk-dynamodb" % versions.aws,
    "com.gu" %% "scanamo" % versions.scanamo
  )

  val circeDependencies = Seq(
    "io.circe" %% "circe-core" % versions.circeVersion,
    "io.circe" %% "circe-generic"% versions.circeVersion,
    "io.circe" %% "circe-generic-extras"% versions.circeVersion,
    "io.circe" %% "circe-parser"% versions.circeVersion,
    "io.circe" %% "circe-java8" % versions.circeVersion
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

  val scalaGraphDependencies = Seq(
    "org.scala-graph" %% "graph-core" % versions.scalaGraphVersion
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
      loggingDependencies ++
      circeDependencies ++ Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka % "test",
    "com.typesafe.akka" %% "akka-stream" % versions.akka % "test"
  ) ++ apacheCommons

  val commonDisplayDependencies = swaggerDependencies ++ guiceDependencies ++ scalacheckDependencies

  val commonElasticsearchDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circeVersion
  ) ++ elasticsearchDependencies ++ guiceDependencies ++ scalacheckDependencies

  val commonMessagingDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-sns" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-sqs" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws,
    "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % versions.akkaStreamAlpakka,
    "io.circe" %% "circe-yaml" % "0.8.0"
  ) ++ WellcomeDependencies.jsonLibrary ++ WellcomeDependencies.monitoringLibrary ++ WellcomeDependencies.storageLibrary ++ akkaDependencies ++ guiceDependencies ++ testDependencies

  val finatraAkkaDependencies = akkaDependencies ++ finatraDependencies ++ guiceDependencies

  val finatraMonitoringDependencies = finatraDependencies ++ WellcomeDependencies.monitoringLibrary

  val finatraStorageDependencies = finatraDependencies ++ WellcomeDependencies.storageLibrary

  val internalModelDependencies = dynamoDependencies ++ Seq(
    "com.github.tototoshi" %% "scala-csv" % versions.scalaCsv
  ) ++ WellcomeDependencies.jsonLibrary

  // Application specific dependency groups
  val idminterDependencies = Seq(
    "org.scalikejdbc" %% "scalikejdbc" % "3.0.0",
    "mysql" % "mysql-connector-java" % "6.0.6",
    "org.flywaydb" % "flyway-core" % "4.2.0",
    "com.amazonaws" % "aws-java-sdk-rds" % versions.aws,
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val snapshotGeneratorDependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakka
  )

  val sierraReaderDependencies: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-optics" % versions.circeVersion,
    "uk.ac.wellcome" %% "sierra-streams-source" % versions.sierraStreamsSourceVersion
  )

  val archiveCommonDependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakka,
    "com.lightbend.akka" %% "akka-stream-alpakka-sns" % versions.akkaStreamAlpakka,
    "org.rogach" %% "scallop" % "3.1.3"
  ) ++ akkaDependencies ++ WellcomeDependencies.storageLibrary ++ WellcomeDependencies.jsonLibrary ++ WellcomeDependencies.monitoringLibrary
}
