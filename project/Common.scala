import sbt._
import Keys._

object Common {
  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.12.6",
    organization := "uk.ac.wellcome",
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlint",
      "-Xverify",
      // It breaks for scala_2.12 because it returns warnings fo unused imports, unused variables and use of deprecated classes
      // TODO turn this back on once the migration is done
//      "-Xfatal-warnings",
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
