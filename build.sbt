import sbt.Keys._
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.sbt._
import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin

def doSharedSetup(project: Project) =
  project
    .dependsOn(common)
    .settings(Common.settings: _*)
    .settings(Finatra.settings: _*)
    .settings(Revolver.settings: _*)
    .settings(EcrPlugin.projectSettings: _*)
    .settings(Packager.settings: _*)
    .settings(DockerCompose.settings: _*)
    .enablePlugins(JavaAppPackaging)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(GitVersioning)

lazy val common = project
  .settings(Common.settings: _*)
  .enablePlugins(GitVersioning)
  .settings(libraryDependencies ++= Dependencies.ingestorDependencies)

lazy val calm_adapter = doSharedSetup(project)
  .settings(Search.settings: _*)
  .settings(libraryDependencies ++= Dependencies.calmAdapterDependencies)

lazy val api = doSharedSetup(project)
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)
  .settings(libraryDependencies ++= Dependencies.apiDependencies)

lazy val ingestor = doSharedSetup(project)
  .settings(Search.settings: _*)
  .settings(libraryDependencies ++= Dependencies.ingestorDependencies)

lazy val transformer = doSharedSetup(project)
  .settings(libraryDependencies ++= Dependencies.transformerDependencies)

lazy val id_minter = doSharedSetup(project)
  .settings(libraryDependencies ++= Dependencies.idminterDependencies)

lazy val root = (project in file("."))
