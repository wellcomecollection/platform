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

lazy val common = doSharedLibrarySetup(project, "sbt_common/common")
  .settings(libraryDependencies ++= Dependencies.commonDependencies)

lazy val internal_model = doSharedLibrarySetup(project, "sbt_common/internal_model")
  .settings(libraryDependencies ++= Dependencies.internalModelDependencies)

lazy val display = doSharedLibrarySetup(project, "sbt_common/display")
  .dependsOn(internal_model % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonDisplayDependencies)

// Elasticsearch depends on some models in the common lib.
lazy val elasticsearch = doSharedLibrarySetup(project, "sbt_common/elasticsearch")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonElasticsearchDependencies)

lazy val messaging = doSharedLibrarySetup(project, "sbt_common/messaging")
  .dependsOn(common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.commonMessagingDependencies)

lazy val finatra_akka = doSharedLibrarySetup(project, "sbt_common/finatra_akka")
  .settings(libraryDependencies ++= Dependencies.finatraAkkaDependencies)

lazy val finatra_controllers = doSharedLibrarySetup(project, "sbt_common/finatra_controllers")
  .settings(libraryDependencies ++= Dependencies.finatraDependencies)

lazy val finatra_elasticsearch = doSharedLibrarySetup(project, "sbt_common/finatra_elasticsearch")
  .dependsOn(elasticsearch % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.finatraAkkaDependencies)

lazy val finatra_messaging = doSharedLibrarySetup(project, "sbt_common/finatra_messaging")
  .dependsOn(messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")
  .dependsOn(finatra_monitoring % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.finatraDependencies)

lazy val finatra_storage = doSharedLibrarySetup(project, "sbt_common/finatra_storage")
  .settings(libraryDependencies ++= Dependencies.finatraStorageDependencies)

lazy val finatra_monitoring = doSharedLibrarySetup(project, "sbt_common/finatra_monitoring")
  .dependsOn(finatra_akka % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.finatraMonitoringDependencies)

lazy val api = doServiceSetup(project, "catalogue_api/api")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(display % "compile->compile;test->test")
  .dependsOn(finatra_akka % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_elasticsearch % "compile->compile;test->test")
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)

lazy val ingestor = doServiceSetup(project, "catalogue_pipeline/ingestor")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_elasticsearch % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .settings(Search.settings: _*)

lazy val transformer_common = doServiceSetup(project, "catalogue_pipeline/transformer/transformer_common")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= WellcomeDependencies.newStorageLibrary)

lazy val transformer_miro = doServiceSetup(project, "catalogue_pipeline/transformer/transformer_miro")
  .dependsOn(transformer_common % "compile->compile;test->test")

lazy val transformer_sierra = doServiceSetup(project, "catalogue_pipeline/transformer/transformer_sierra")
  .dependsOn(transformer_common % "compile->compile;test->test")

lazy val merger = doServiceSetup(project, "catalogue_pipeline/merger")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= WellcomeDependencies.newStorageLibrary)

lazy val id_minter = doServiceSetup(project, "catalogue_pipeline/id_minter")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.idminterDependencies)

lazy val recorder = doServiceSetup(project, "catalogue_pipeline/recorder")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= WellcomeDependencies.newStorageLibrary)

lazy val matcher = doServiceSetup(project, "catalogue_pipeline/matcher")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")
  .dependsOn(finatra_akka % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.scalaGraphDependencies)

lazy val reindex_request_creator = doServiceSetup(project, "reindexer/reindex_request_creator")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")

lazy val goobi_reader = doServiceSetup(project, "goobi_adapter/goobi_reader")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")

lazy val sierra_adapter_common = doServiceSetup(project, "sierra_adapter/common")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= WellcomeDependencies.newStorageLibrary)

lazy val sierra_reader = doSharedSierraSetup(project, "sierra_adapter/sierra_reader")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.sierraReaderDependencies)

lazy val sierra_items_to_dynamo = doSharedSierraSetup(project, "sierra_adapter/sierra_items_to_dynamo")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")

lazy val sierra_bib_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_bib_merger")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")

lazy val sierra_item_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_item_merger")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .dependsOn(finatra_storage % "compile->compile;test->test")

lazy val snapshot_generator = doServiceSetup(project, "data_api/snapshot_generator")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(display % "compile->compile;test->test")
  .dependsOn(finatra_controllers % "compile->compile;test->test")
  .dependsOn(finatra_elasticsearch % "compile->compile;test->test")
  .dependsOn(finatra_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.snapshotGeneratorDependencies)

lazy val archive_common = doServiceSetup(project, "archive/common")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.archiveCommonDependencies)

lazy val archivist = doServiceSetup(project, "archive/archivist")
  .dependsOn(archive_common % "compile->compile;test->test")

lazy val registrar = doServiceSetup(project, "archive/registrar")
  .dependsOn(archive_common % "compile->compile;test->test")

lazy val root = (project in file("."))
  .aggregate(
    common,

    internal_model,
    display,
    elasticsearch,
    messaging,

    finatra_akka,
    finatra_controllers,
    finatra_elasticsearch,
    finatra_messaging,
    finatra_monitoring,
    finatra_storage,

    api,
    ingestor,
    transformer_common,
    transformer_miro,
    transformer_sierra,
    id_minter,
    recorder,
    matcher,
    merger,

    reindex_request_creator,

    goobi_reader,
    sierra_adapter_common,
    sierra_reader,
    sierra_items_to_dynamo,
    sierra_bib_merger,
    sierra_item_merger,
    snapshot_generator,

    archive_common,
    archivist,
    registrar
  )
