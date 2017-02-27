import sbt._
import Keys._

import com.typesafe.sbt.SbtGit._


object Common {
  val settings: Seq[Def.Setting[_]] = Seq(

    scalaVersion := "2.11.8",
    organization := "uk.ac.wellcome",
    git.baseVersion := "0.0.1",

    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += "maven.twttr.com" at "https://maven.twttr.com",
    resolvers += "elasticsearch-releases" at "https://artifacts.elastic.co/maven",

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
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
  )
}
