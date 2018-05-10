import sbt._

object Dependencies {

  lazy val versions = new {
    val akka = "2.4.17"
    val akkaStreamAlpakkaS3 = "0.17"
    val aws = "1.11.95"
    val apacheLogging = "2.8.2"
    val finatra = "2.8.0"
    val guice = "4.0"
    val logback = "1.1.8"
    val mockito = "1.9.5"
    val scalatest = "3.0.1"
    val junitInterface = "0.11"
    val elastic4s = "5.6.5"
    val scanamo = "1.0.0-M3"
    val jacksonYamlVersion = "2.8.8"
    val scalaJacksonVersion = "2.9.5"
    val circeVersion = "0.9.0"
    val scalaCheckVersion = "1.13.4"
    val scalaCheckShapelessVersion = "1.1.6"
    val sierraStreamsSourceVersion = "0.2"
    val jaxbVersion = "2.2.11"
  }

  val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-agent" % versions.akka,
    "com.typesafe.akka" %% "akka-stream" % versions.akka
  )

  val scanamoDependencies = Seq(
    "com.gu" %% "scanamo" % versions.scanamo
  )

  val mysqlDependencies = Seq(
    "org.scalikejdbc" %% "scalikejdbc" % "3.0.0",
    "mysql" % "mysql-connector-java" % "6.0.6",
    "org.flywaydb" % "flyway-core" % "4.2.0"
  )

  val esDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-core" % versions.apacheLogging,
    "org.apache.logging.log4j" % "log4j-api" % versions.apacheLogging,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
  )

  val commonElasticsearchFinatraDependencies =
    esDependencies ++ finatraDependencies

  val circeDependencies = Seq(
    "io.circe" %% "circe-core" % versions.circeVersion,
    "io.circe" %% "circe-generic"% versions.circeVersion,
    "io.circe" %% "circe-generic-extras"% versions.circeVersion,
    "io.circe" %% "circe-parser"% versions.circeVersion,
    "io.circe" %% "circe-java8" % versions.circeVersion,
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val jacksonDependencies = Seq(
    "javax.xml.bind" % "jaxb-api" % versions.jaxbVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % versions.jacksonYamlVersion % " test",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.scalaJacksonVersion % " test"
  )

  val swaggerDependencies = Seq(
    "com.github.xiaodongw" %% "swagger-finatra" % "0.7.2"
  )

  val sharedDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val apiDependencies = Seq(
    "org.scalacheck" %% "scalacheck" % versions.scalaCheckVersion % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalaCheckShapelessVersion % "test"
  )

  val loggingDependencies = Seq(
    "org.clapper" %% "grizzled-slf4j" % "1.3.2"
  )

  val finatraDependencies = Seq(
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

  val commonDependencies = Seq(
    "com.google.inject" % "guice" % versions.guice,
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "com.novocode" % "junit-interface" % versions.junitInterface % "test",
    "com.typesafe.akka" %% "akka-actor" % versions.akka % "test",
    "com.typesafe.akka" %% "akka-agent" % versions.akka % "test",
    "com.typesafe.akka" %% "akka-stream" % versions.akka % "test"
  ) ++ circeDependencies ++ loggingDependencies

  val pipelineModelDependencies = circeDependencies ++ scanamoDependencies

  val commonFinatraDependencies =
    commonDependencies ++ finatraDependencies ++ akkaDependencies

  // We use Circe for all our JSON serialisation, but our local SNS container
  // returns YAML, and currently we use Jackson to parse that YAML.
  // TODO: Rewrite the SNS fixture to use https://github.com/circe/circe-yaml
  val commonMessagingDependencies = commonDependencies ++ jacksonDependencies ++ Seq(
    "com.amazonaws" % "aws-java-sdk-dynamodb" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-sns" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-sqs" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws,
    "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % versions.akkaStreamAlpakkaS3
  ) ++ loggingDependencies ++ jacksonDependencies

  val commonStorageDependencies = commonDependencies ++ scanamoDependencies ++ Seq(
    "com.amazonaws" % "aws-java-sdk-dynamodb" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws
  ) ++ loggingDependencies

  val commonMonitoringDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-cloudwatch" % versions.aws
  ) ++ akkaDependencies

  val sierraAdapterCommonDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val idminterDependencies =
    mysqlDependencies ++ Seq(
    "com.amazonaws" % "aws-java-sdk-rds" % versions.aws,
    "io.circe" %% "circe-optics" % versions.circeVersion
  )

  val snapshotGeneratorDependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakkaS3
  )

  val sierraReaderDependencies =Seq(
    "uk.ac.wellcome" %% "sierra-streams-source" % versions.sierraStreamsSourceVersion
  )
}
