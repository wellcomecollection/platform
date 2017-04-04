import sbt._
import Keys._

import com.typesafe.sbt.SbtGit._

object Common {
  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.8",
    organization := "uk.ac.wellcome",
    git.baseVersion := "0.0.1",
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlint",
      "-Yclosure-elim",
      "-Yinline",
      "-Xverify",
      "-feature",
      "-language:postfixOps"
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
  ) ++ Search.settings ++ Swagger.settings ++ Finatra.settings ++ DynamoDBLocal.settings
}

object Swagger {
  val settings: Seq[Def.Setting[_]] = Seq(
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases/"
  )
}

object DynamoDBLocal {
  val settings: Seq[Def.Setting[_]] = Seq(
    resolvers +=
      "DynamoDB Local Release Repository" at "https://s3-us-west-2.amazonaws.com/dynamodb-local/release"
  )
}

object Search {
  val settings: Seq[Def.Setting[_]] = Seq(
    resolvers += "elasticsearch-releases" at "https://artifacts.elastic.co/maven"
  )
}

object Finatra {
  val settings: Seq[Def.Setting[_]] = Seq(
    resolvers += "maven.twttr.com" at "https://maven.twttr.com",
    fork in run := true
  )
}
