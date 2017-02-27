import sbt.Keys._
import com.amazonaws.regions.{Region, Regions}

scalaVersion := "2.11.8"

fork in run := true

enablePlugins(JavaAppPackaging)
enablePlugins(GitVersioning)

useJGit

Revolver.settings

lazy val common = (project in file("common-lib")).
  settings(Common.settings: _*)

lazy val api = (project in file("api")).
  dependsOn(common).
  settings(EcrPlugin.projectSettings: _*).
  settings(Packager.settings: _*).
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.apiDependencies)

lazy val root = (project in file("."))
