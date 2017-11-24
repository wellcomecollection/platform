import sbt.Keys._

import java.io.File

def doSharedSetup(project: Project, folder: String) =
  project
    .in(new File(folder))
    .dependsOn(common % "compile->compile;test->test")
    .settings(Common.settings: _*)
    .settings(Finatra.settings: _*)
    .settings(Revolver.settings: _*)
    .settings(DockerCompose.settings: _*)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(JavaAppPackaging)

lazy val common = project
  .settings(Common.settings: _*)
  .settings(DockerCompose.settings: _*)
  .enablePlugins(DockerComposePlugin)
  .settings(libraryDependencies ++= Dependencies.ingestorDependencies)

lazy val api = doSharedSetup(project, "catalogue_api/api")
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)
  .settings(libraryDependencies ++= Dependencies.apiDependencies)

lazy val ingestor = doSharedSetup(project, "catalogue_pipeline/ingestor")
  .settings(Search.settings: _*)
  .settings(libraryDependencies ++= Dependencies.ingestorDependencies)

lazy val transformer = doSharedSetup(project, "catalogue_pipeline/transformer")
  .settings(libraryDependencies ++= Dependencies.transformerDependencies)

lazy val id_minter = doSharedSetup(project, "catalogue_pipeline/id_minter")
  .settings(libraryDependencies ++= Dependencies.idminterDependencies)

lazy val reindexer = doSharedSetup(project, "catalogue_pipeline/reindexer")
  .settings(libraryDependencies ++= Dependencies.reindexerDependencies)

lazy val sierra_to_dynamo = doSharedSetup(project, "sierra_adapter/sierra_to_dynamo")
  .settings(libraryDependencies ++= Dependencies.sierraToDynamoDepedencies)

lazy val root = (project in file("."))
  .aggregate(common,
             api,
             ingestor,
             transformer,
             id_minter,
             reindexer, sierra_to_dynamo)
