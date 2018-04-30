import sbt._
import Keys._

object Common {
  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.8",
    organization := "uk.ac.wellcome",
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlint",
      "-Xverify",
      "-Xfatal-warnings",
      "-feature",
      "-language:postfixOps"
    ),
    parallelExecution in Test := false,
    libraryDependencies ++= Dependencies.sharedDependencies
  ) ++
    Search.settings ++
    Swagger.settings ++
    Finatra.settings
}

object Swagger {
  val settings: Seq[Def.Setting[_]] = Seq(
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases/"
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
