import sbt._

object Dependencies {

  lazy val versions = new {
    val akka = "2.4.17"
    val akkaStreamAlpakkaS3 = "0.17"
    val aws = "1.11.95"
    val apacheLogging = "2.8.2"
    val finatra = "2.10.0"
    val guice = "4.0"
    val logback = "1.1.8"
    val mockito = "1.9.5"
    val scalatest = "3.0.1"
    val junitInterface = "0.11"
    val elastic4s = "5.6.5"
    val scanamo = "1.0.0-M3"
    val circeVersion = "0.9.0"
    val scalaCheckVersion = "1.13.4"
    val scalaCheckShapelessVersion = "1.1.6"
    val sierraStreamsSourceVersion = "0.2"
    val jaxbVersion = "2.2.11"
  }

  // External Library dependency groups
  val akkaDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-agent" % versions.akka,
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

  val sharedDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val scalacheckDependencies = Seq(
    "org.scalacheck" %% "scalacheck" % versions.scalaCheckVersion % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalaCheckShapelessVersion % "test"
  )

  val finatraDependencies: Seq[ModuleID] = Seq(
    "com.twitter" %% "finatra-http" % versions.finatra,
    "com.twitter" %% "finatra-httpclient" % versions.finatra,
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "com.twitter" %% "finatra-http" % versions.finatra % "test",
    "com.twitter" %% "finatra-jackson" % versions.finatra % "test",
    "com.twitter" %% "inject-server" % versions.finatra % "test",
    "com.twitter" %% "inject-app" % versions.finatra % "test",
    "com.twitter" %% "inject-core" % versions.finatra % "test",
    "com.twitter" %% "inject-modules" % versions.finatra % "test",
    "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",
    "com.twitter" %% "finatra-http" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "finatra-jackson" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
    "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests"
  )

  val elasticsearchDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-core" % versions.apacheLogging,
    "org.apache.logging.log4j" % "log4j-api" % versions.apacheLogging,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
  )

  // Internal Library dependency groups
  val commonDependencies: Seq[ModuleID] = Seq(
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "com.novocode" % "junit-interface" % versions.junitInterface % "test",
    "javax.xml.bind" % "jaxb-api" % versions.jaxbVersion
  ) ++ finatraDependencies ++ akkaDependencies ++ circeDependencies

  val commonDisplayDependencies: Seq[ModuleID] = swaggerDependencies

  val commonElasticsearchDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circeVersion
  ) ++ elasticsearchDependencies ++ scalacheckDependencies

  val commonMessagingDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-sns" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-sqs" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws,
    "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % versions.akkaStreamAlpakkaS3,
    "io.circe" %% "circe-yaml" % "0.8.0"
  ) ++ dynamoDependencies

  val commonStorageDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws
  ) ++ dynamoDependencies

  val commonMonitoringDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-cloudwatch" % versions.aws
  )

  val internalModelDependencies = dynamoDependencies

  // Application specific dependency groups
  val sierraAdapterCommonDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val idminterDependencies = Seq(
    "org.scalikejdbc" %% "scalikejdbc" % "3.0.0",
    "mysql" % "mysql-connector-java" % "6.0.6",
    "org.flywaydb" % "flyway-core" % "4.2.0",
    "com.amazonaws" % "aws-java-sdk-rds" % versions.aws,
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val snapshotGeneratorDependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakkaS3
  )

  val sierraReaderDependencies: Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% "sierra-streams-source" % versions.sierraStreamsSourceVersion
  )
}
