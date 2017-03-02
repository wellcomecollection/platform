import sbt.Keys._
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.sbt._
import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.packager.linux.LinuxPlugin

lazy val common = (project in file("common-lib")).
  settings(SbtGit.useJGit).
  settings(Common.settings: _*).
  settings(Search.settings: _*).
  settings(Finatra.settings: _*).
  settings(GitVersioning.buildSettings: _*).
  settings(libraryDependencies ++= Dependencies.commonDependencies)

lazy val api = (project in file("api")).
  dependsOn(common).
  settings(Search.settings: _*).
  settings(Finatra.settings: _*).
  settings(Revolver.settings: _*).
  settings(EcrPlugin.projectSettings: _*).
  settings(SbtNativePackager.projectSettings: _*).
  settings(LinuxPlugin.projectSettings: _*).
  settings(UniversalPlugin.projectSettings: _*).
  settings(JavaAppPackaging.projectSettings: _*).
  settings(Packager.settings: _*).
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.apiDependencies)

lazy val root = (project in file("."))
