import sbt.Keys._
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.sbt._
import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin

lazy val common = project
  .settings(Common.settings: _*)
  .enablePlugins(GitVersioning)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)

lazy val api = project
  .dependsOn(common)
  .settings(Common.settings: _*)
  .settings(Finatra.settings: _*)
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)
  .settings(Revolver.settings: _*)
  .settings(EcrPlugin.projectSettings: _*)
  .settings(Packager.settings: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GitVersioning)
  .settings(libraryDependencies ++= Dependencies.apiDependencies)

lazy val transformer = project
  .dependsOn(common)
  .settings(Common.settings: _*)
  .settings(Finatra.settings: _*)
  .settings(Revolver.settings: _*)
  .settings(EcrPlugin.projectSettings: _*)
  .settings(Packager.settings: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GitVersioning)
  .settings(libraryDependencies ++= Dependencies.transformerDependencies)

lazy val calm_adapter = project.
  dependsOn(common).
  settings(Common.settings: _*).
  settings(Finatra.settings: _*).
  settings(Search.settings: _*).
  settings(Revolver.settings: _*).
  settings(EcrPlugin.projectSettings: _*).
  settings(Packager.settings: _*).
  enablePlugins(JavaAppPackaging).
  enablePlugins(GitVersioning).
  settings(libraryDependencies ++= Dependencies.calmAdapterDependencies)

lazy val transformer = project.
  dependsOn(common).
  settings(Common.settings: _*).
  settings(Finatra.settings: _*).
  settings(Revolver.settings: _*).
  settings(EcrPlugin.projectSettings: _*).
  settings(Packager.settings: _*).
  enablePlugins(JavaAppPackaging).
  enablePlugins(GitVersioning).
  settings(libraryDependencies ++= Dependencies.transformerDependencies)

lazy val root = (project in file("."))
