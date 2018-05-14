import sbt.Keys._

import java.io.File

def doServiceSetup(project: Project, folder: String) =
  project
    .in(new File(folder))
    .dependsOn(common % "compile->compile;test->test")
    .settings(Common.settings: _*)
    .settings(Finatra.settings: _*)
    .settings(DockerCompose.settings: _*)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(JavaAppPackaging)

def doSharedLibrarySetup(project: Project, folder: String) =
  project
    .in(new File(folder))
    .settings(Common.settings: _*)
    .settings(DockerCompose.settings: _*)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(JavaAppPackaging)

def doSharedSierraSetup(project: Project, folder: String) =
  doServiceSetup(project = project, folder = folder)
    .dependsOn(sierra_adapter_common % "compile->compile;test->test")

lazy val common = project
  .settings(Common.settings: _*)
  .settings(DockerCompose.settings: _*)
  .enablePlugins(DockerComposePlugin)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)

// It depends on common because it uses JsonUtil
lazy val internal_model = doSharedLibrarySetup(project, "sbt_common/internal_model")
  .dependsOn(common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.internalModelDependencies)

lazy val common_display = doSharedLibrarySetup(project, "sbt_common/display")
  .dependsOn(internal_model % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonDisplayDependencies)

lazy val common_elasticsearch = doSharedLibrarySetup(project, "sbt_common/elasticsearch")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonElasticsearchDependencies)

lazy val common_monitoring = doSharedLibrarySetup(project, "sbt_common/monitoring")
  .dependsOn(common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonMonitoringDependencies)

// Messaging depends on the S3ObjectStore for message pointers and AWSConfig.
lazy val common_messaging = doSharedLibrarySetup(project, "sbt_common/messaging")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(common_monitoring % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonMessagingDependencies)

// Storage depends on AWSConfig.
lazy val common_storage = doSharedLibrarySetup(project, "sbt_common/storage")
  .dependsOn(common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonStorageDependencies)

lazy val api = doServiceSetup(project, "catalogue_api/api")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common_display % "compile->compile;test->test")
  .dependsOn(common_elasticsearch % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.apiDependencies)
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)

lazy val ingestor = doServiceSetup(project, "catalogue_pipeline/ingestor")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common_elasticsearch % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .settings(Search.settings: _*)

lazy val transformer = doServiceSetup(project, "catalogue_pipeline/transformer")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")

lazy val id_minter = doServiceSetup(project, "catalogue_pipeline/id_minter")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.idminterDependencies)

lazy val recorder = doServiceSetup(project, "catalogue_pipeline/recorder")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")

lazy val reindex_worker = doServiceSetup(project, "reindexer/reindex_worker")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")

lazy val sierra_adapter_common = doServiceSetup(project, "sierra_adapter/common")
  .dependsOn(common_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.sierraAdapterCommonDependencies)

lazy val sierra_reader = doSharedSierraSetup(project, "sierra_adapter/sierra_reader")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.sierraReaderDependencies)

lazy val sierra_items_to_dynamo = doSharedSierraSetup(project, "sierra_adapter/sierra_items_to_dynamo")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")

lazy val sierra_bib_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_bib_merger")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")

lazy val sierra_item_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_item_merger")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .dependsOn(common_storage % "compile->compile;test->test")

lazy val snapshot_generator = doServiceSetup(project, "data_api/snapshot_generator")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common_display % "compile->compile;test->test")
  .dependsOn(common_elasticsearch % "compile->compile;test->test")
  .dependsOn(common_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.snapshotGeneratorDependencies)

lazy val root = (project in file("."))
  .aggregate(
    common,
    internal_model,
    common_display,
    common_elasticsearch,
    common_messaging,
    common_monitoring,
    common_storage,
    api,
    ingestor,
    transformer,
    id_minter,
    reindex_worker,
    sierra_adapter_common,
    sierra_reader,
    sierra_items_to_dynamo,
    sierra_bib_merger,
    sierra_item_merger,
    snapshot_generator
  )
