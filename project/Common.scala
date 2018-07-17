import sbt._
import Keys._

object Common {
  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.12.6",
    organization := "uk.ac.wellcome",

    // We have to do this slightly awkward configuration of resolvers because
    // we have a small number of releases on Maven Central with conflicting
    // version numbers -- we *don't* want to pick those up, we want our
    // S3-published packages first.
    fullResolvers :=
      Seq(
        Resolver.url(
          "S3 releases",
          url("http://releases.mvn-repo.wellcomecollection.org.s3-website-eu-west-1.amazonaws.com"))(Resolver.ivyStylePatterns)
      ) ++ fullResolvers.value ++ Seq(Resolver.sonatypeRepo("releases")),

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
