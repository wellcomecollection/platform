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

lazy val config_core = doSharedLibrarySetup(project, "sbt_common/config/core")
  .dependsOn(common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.typesafeStorageDependencies)

lazy val config_storage = doSharedLibrarySetup(project, "sbt_common/config/storage")
  .dependsOn(config_core % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.typesafeStorageDependencies)

lazy val config_monitoring = doSharedLibrarySetup(project, "sbt_common/config/monitoring")
  .dependsOn(config_core % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.typesafeMonitoringDependencies)

lazy val config_messaging = doSharedLibrarySetup(project, "sbt_common/config/messaging")
  .dependsOn(config_core % "compile->compile")
  .dependsOn(config_monitoring % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.configMessagingDependencies)

lazy val config_elasticsearch = doSharedLibrarySetup(project, "sbt_common/config/elasticsearch")
  .dependsOn(config_core % "compile->compile;test->test")
  .dependsOn(elasticsearch % "compile->compile;test->test")

lazy val api = doServiceSetup(project, "catalogue_api/api")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(display % "compile->compile;test->test")
  .dependsOn(elasticsearch % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.apiDependencies)
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)

lazy val ingestor = doServiceSetup(project, "catalogue_pipeline/ingestor")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_elasticsearch % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")
  .settings(Search.settings: _*)

lazy val transformer_miro = doServiceSetup(project, "catalogue_pipeline/transformer/transformer_miro")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.miroTransformerDependencies)

lazy val transformer_sierra = doServiceSetup(project, "catalogue_pipeline/transformer/transformer_sierra")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")

lazy val merger = doServiceSetup(project, "catalogue_pipeline/merger")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")

lazy val id_minter = doServiceSetup(project, "catalogue_pipeline/id_minter")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.idminterDependencies)

lazy val recorder = doServiceSetup(project, "catalogue_pipeline/recorder")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")

lazy val matcher = doServiceSetup(project, "catalogue_pipeline/matcher")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.scalaGraphDependencies)

lazy val reindex_worker = doServiceSetup(project, "reindexer/reindex_worker")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")

lazy val goobi_reader = doServiceSetup(project, "goobi_adapter/goobi_reader")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= WellcomeDependencies.jsonLibrary)

lazy val sierra_adapter_common = doServiceSetup(project, "sierra_adapter/common")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")

lazy val sierra_reader = doSharedSierraSetup(project, "sierra_adapter/sierra_reader")
  .dependsOn(common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.sierraReaderDependencies)

lazy val sierra_items_to_dynamo = doSharedSierraSetup(project, "sierra_adapter/sierra_items_to_dynamo")
  .dependsOn(common % "compile->compile;test->test")

lazy val sierra_bib_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_bib_merger")
  .dependsOn(common % "compile->compile;test->test")

lazy val sierra_item_merger = doSharedSierraSetup(project, "sierra_adapter/sierra_item_merger")
  .dependsOn(common % "compile->compile;test->test")

lazy val snapshot_generator = doServiceSetup(project, "data_api/snapshot_generator")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal_model % "compile->compile;test->test")
  .dependsOn(display % "compile->compile;test->test")
  .dependsOn(config_elasticsearch % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.snapshotGeneratorDependencies)

lazy val storage_common = doServiceSetup(project, "storage/common")
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(config_messaging % "compile->compile;test->test")
  .dependsOn(config_storage % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.storageCommonDependencies)

lazy val storage_display = doServiceSetup(project, "storage/display")
  .dependsOn(storage_common % "compile->compile;test->test")

lazy val archivist = doServiceSetup(project, "storage/archivist")
  .dependsOn(storage_common % "compile->compile;test->test")

lazy val notifier = doServiceSetup(project, "storage/notifier")
  .dependsOn(storage_common % "compile->compile;test->test")
  .dependsOn(storage_display % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.wiremockDependencies)

lazy val bags_common = doServiceSetup(project, "storage/bags_common")
  .dependsOn(storage_common % "compile->compile;test->test")

lazy val bags = doServiceSetup(project, "storage/bags")
  .dependsOn(bags_common % "compile->compile;test->test")

lazy val bags_api = doServiceSetup(project, "storage/bags_api")
  .dependsOn(bags_common % "compile->compile;test->test")
  .dependsOn(storage_display % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.registrarHttpDependencies)

lazy val ingests_common = doServiceSetup(project, "storage/ingests_common")
  .dependsOn(storage_common % "compile->compile;test->test")

lazy val ingests = doServiceSetup(project, "storage/ingests")
  .dependsOn(ingests_common % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.wiremockDependencies)

lazy val ingests_api = doServiceSetup(project, "storage/ingests_api")
  .dependsOn(ingests_common % "compile->compile;test->test")
  .dependsOn(storage_display % "compile->compile;test->test")
  .settings(libraryDependencies ++= Dependencies.progressHttpDependencies)

lazy val bag_replicator = doServiceSetup(project, "storage/bag_replicator")
  .dependsOn(storage_common % "compile->compile;test->test")

lazy val root = (project in file("."))
  .aggregate(
    common,

    internal_model,
    display,
    elasticsearch,

    config_core,
    config_messaging,
    config_monitoring,
    config_storage,

    api,
    ingestor,
    transformer_miro,
    transformer_sierra,
    id_minter,
    recorder,
    matcher,
    merger,

    reindex_worker,

    goobi_reader,
    sierra_adapter_common,
    sierra_reader,
    sierra_items_to_dynamo,
    sierra_bib_merger,
    sierra_item_merger,
    snapshot_generator,

    storage_common,
    storage_display,
    archivist,
    bag_replicator,
    notifier,
    ingests_api,
    ingests,
    bags_common,
    bags_api,
    bags
  )
