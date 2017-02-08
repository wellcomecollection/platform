import sbt.Keys._
import com.amazonaws.regions.{Region, Regions}

scalaVersion := "2.11.8"

fork in run := true

enablePlugins(JavaAppPackaging)
enablePlugins(GitVersioning)

useJGit

resolvers += Resolver.sonatypeRepo("releases")
resolvers += "maven.twttr.com" at "https://maven.twttr.com"

Revolver.settings

name := "platform"
organization := "uk.ac.wellcome"
git.baseVersion := "0.0.1"

lazy val versions = new {
  val finatra = "2.7.0"
  val guice = "4.0"
  val logback = "1.1.8"
  val mockito = "1.9.5"
  val scalatest = "3.0.1"
  val junitInterface = "0.11"
}

libraryDependencies ++= Seq(
  "com.twitter" %% "finatra-http" % versions.finatra,
  "com.twitter" %% "finatra-httpclient" % versions.finatra,
  "ch.qos.logback" % "logback-classic" % versions.logback,

  "org.elasticsearch" % "elasticsearch" % "5.1.2",
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.1.5",
  "com.sksamuel.elastic4s" %% "elastic4s-xpack-security" % "5.1.5",

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
  "com.novocode" % "junit-interface" % versions.junitInterface % "test"
  )

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-Xlint",
  "-Yclosure-elim",
  "-Yinline",
  "-Xverify",
  "-feature",
  "-language:postfixOps"
)

dockerBaseImage := "anapsix/alpine-java"

region           in ecr := Region.getRegion(Regions.EU_WEST_1)
repositoryName   in ecr := s"${organization.value}/${name.value}:${version.value}"
localDockerImage in ecr := s"${name.value}:${version.value}"

push in ecr := (push in ecr dependsOn (publishLocal in Docker, login in ecr)).value

// TODO: Move to seperate plugin
import complete.DefaultParsers._
import scala.util.Try
import scala.collection.JavaConverters._
import java.io.{File, InputStream}
import java.nio.file.Files
import java.nio.file.StandardCopyOption._
import com.typesafe.config._
import com.amazonaws.services.s3.AmazonS3ClientBuilder


lazy val deployStage = SettingKey[String]("deploy-env","Deploy environment.")
lazy val settingsBucket = SettingKey[String]("settings-bucket","Settings bucket.")
lazy val configure = taskKey[Seq[String]]("Containerise application for deployment.")

configure := {
  val bucket    = settingsBucket.value
  val stage     = deployStage.value

  val key       = s"config/${stage}/platform.conf"
  val localPath = s"conf/application.${stage}.conf"

  if(stage != "dev") {
    val s3Client = AmazonS3ClientBuilder.defaultClient()
    val s3Object = s3Client.getObject(bucket, key)

    val fileStream = s3Object.getObjectContent().asInstanceOf[InputStream]
    val targetFile = new File(localPath)
    val targetPath = targetFile.toPath()

    Files.copy(fileStream, targetPath, REPLACE_EXISTING)
    fileStream.close()
  }

  val confSet = ConfigFactory
    .parseFile(new File(localPath))
    .resolve()
    .entrySet()
    .asScala

  confSet.map(entry =>
      s"-D${entry.getKey()}=${entry.getValue().render()}")
    .toSeq
}

javaOptions in Universal ++= configure.value
