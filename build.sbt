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

def doSharedSierraSetup(project: Project, folder: String) =
  doSharedSetup(project = project, folder = folder)
    .dependsOn(sierra_adapter_common % "compile->compile;test->test")

lazy val sierra_adapter_common = doSharedSetup(project, "sierra_adapter/common")
  .settings(libraryDependencies ++= Dependencies.sierraAdapterCommonDependencies)

lazy val sierra_reader = doSharedSierraSetup(project, "sierra_adapter/sierra_reader")
  .settings(libraryDependencies ++= Dependencies.sierraReaderDependencies)

lazy val sierra_items_to_dynamo = doSharedSierraSetup(project, "sierra_adapter/sierra_items_to_dynamo")

lazy val sierra_bib_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_bib_merger")
  .settings(libraryDependencies ++= Dependencies.sierraBibMergerDepedencies)

lazy val sierra_item_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_item_merger")
  .settings(libraryDependencies ++= Dependencies.sierraItemMergerDependencies)

lazy val root = (project in file("."))
  .aggregate(
    common,
    api,
    ingestor,
    transformer,
    id_minter,
    reindexer,
    sierra_adapter_common,
    sierra_reader,
    sierra_items_to_dynamo,
    sierra_bib_merger,
    sierra_item_merger
  )
