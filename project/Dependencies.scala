import sbt._

object Dependencies {

  lazy val versions = new {
    val akka = "2.4.17"
    val aws = "1.11.95"
    val apacheLogging = "2.8.2"
    val finatra = "2.8.0"
    val guice = "4.0"
    val logback = "1.1.8"
    val mockito = "1.9.5"
    val scalatest = "3.0.1"
    val junitInterface = "0.11"
    val elastic4s = "5.4.1"
    val scanamo = "1.0.0-M3"
    val jacksonYamlVersion = "2.8.8"
    val circeVersion = "0.9.0"
    val scalaCheckVersion = "1.13.4"
    val scalaCheckShapelessVersion = "1.1.6"
    val sierraStreamsSourceVersion = "0.2"
  }

  val akkaDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-agent" % versions.akka,
    "com.typesafe.akka" %% "akka-stream" % versions.akka
  )

  val awsDependencies: Seq[ModuleID] = Seq(
    "com.amazonaws" % "aws-java-sdk" % versions.aws
  )

  val dynamoDependencies: Seq[ModuleID] = Seq(
    "com.gu" %% "scanamo" % versions.scanamo
  )

  val mysqlDependencies: Seq[ModuleID] = Seq(
    "org.scalikejdbc" %% "scalikejdbc" % "3.0.0",
    "mysql" % "mysql-connector-java" % "6.0.6",
    "org.flywaydb" % "flyway-core" % "4.2.0"
  )
  val esDependencies: Seq[ModuleID] = Seq(
    "org.apache.logging.log4j" % "log4j-core" % versions.apacheLogging,
    "org.apache.logging.log4j" % "log4j-api" % versions.apacheLogging,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
  )

  val circeDependencies = Seq(
    "io.circe" %% "circe-core" % versions.circeVersion,
    "io.circe" %% "circe-generic"% versions.circeVersion,
    "io.circe" %% "circe-generic-extras"% versions.circeVersion,
    "io.circe" %% "circe-parser"% versions.circeVersion,
    "io.circe" %% "circe-optics" % versions.circeVersion,
    "io.circe" %% "circe-java8" % versions.circeVersion
  )

  val jacksonDependencies = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % versions.jacksonYamlVersion % " test"
  )

  val swaggerDependencies = Seq(
    "com.github.xiaodongw" %% "swagger-finatra" % "0.7.2"
  )

  val commonDependencies: Seq[ModuleID] = Seq(
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
    "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "org.scalatest" %% "scalatest" % versions.scalatest % "test",
    "com.novocode" % "junit-interface" % versions.junitInterface % "test",
    "org.scalacheck" %% "scalacheck" % versions.scalaCheckVersion % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalaCheckShapelessVersion % "test"
  ) ++ esDependencies ++ awsDependencies ++ akkaDependencies ++ dynamoDependencies ++ jacksonDependencies ++ circeDependencies ++ swaggerDependencies

  val sierraAdapterCommonDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  ) ++ dynamoDependencies

  val apiDependencies: Seq[ModuleID] = commonDependencies

  val transformerDependencies
    : Seq[ModuleID] = commonDependencies ++ akkaDependencies

  val calmAdapterDependencies
    : Seq[ModuleID] = commonDependencies ++ akkaDependencies

  val ingestorDependencies: Seq[ModuleID] = commonDependencies

  val idminterDependencies
    : Seq[ModuleID] = commonDependencies ++ mysqlDependencies

  val reindexerDependencies: Seq[ModuleID] = commonDependencies

  val recorderDependencies: Seq[ModuleID] = Seq()

  val sierraReaderDependencies: Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% "sierra-streams-source" % versions.sierraStreamsSourceVersion
  )

  val sierraBibMergerDepedencies: Seq[ModuleID] = commonDependencies

  val sierraItemMergerDependencies: Seq[ModuleID] = commonDependencies
}
