import sbt.Keys._

import java.io.File

def setupProject(
  project: Project,
  folder: String,
  localDependencies: Seq[Project] = Seq(),
  externalDependencies: Seq[ModuleID] = Seq()
): Project = {

  // Here we write a bit of metadata about the project, and the other
  // local projects it depends on.  This can be picked up by some hypothetical
  // future build system to determine whether to run tests based on the
  // up-to-date project graph.
  // See https://www.scala-sbt.org/release/docs/Howto-Generating-Files.html
  val file = new File(s"builds/sbt_metadata/${project.id}.json")
  val dependenciesJson = localDependencies
    .map { p: Project => p.id }
    .map { id: String => s""""$id""""}
    .mkString(", ")

  IO.write(file, s"""
    |{
    |  "id": "${project.id}",
    |  "folder": "${folder}",
    |  "dependsOn": [${dependenciesJson}]
    |}
  """.stripMargin.trim)

  // And here we actually create the project, with a few convenience wrappers
  // to make defining projects below cleaner.
  val dependsOn = localDependencies
    .map { project: Project =>
      ClasspathDependency(
        project = project,
        configuration = Some("compile->compile;test->test")
      )
    }

  project
    .in(new File(folder))
    .settings(Common.settings: _*)
    .settings(DockerCompose.settings: _*)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(JavaAppPackaging)
    .dependsOn(dependsOn: _*)
    .settings(libraryDependencies ++= externalDependencies)
}

lazy val common = setupProject(project, "sbt_common/common",
  externalDependencies = Dependencies.commonDependencies
)

lazy val internal_model = setupProject(project, "sbt_common/internal_model",
  externalDependencies = Dependencies.internalModelDependencies
)

lazy val display = setupProject(project, "sbt_common/display",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.commonDisplayDependencies
)

// Elasticsearch depends on some models in the common lib.
lazy val elasticsearch = setupProject(project, "sbt_common/elasticsearch",
  localDependencies = Seq(common, internal_model),
  externalDependencies = Dependencies.commonElasticsearchDependencies
)

lazy val config_core = setupProject(project, "sbt_common/config/core",
  localDependencies = Seq(common),
  externalDependencies = Dependencies.typesafeCoreDependencies
)

lazy val config_storage = setupProject(project, "sbt_common/config/storage",
  localDependencies = Seq(config_core),
  externalDependencies = Dependencies.typesafeStorageDependencies
)

lazy val config_monitoring = setupProject(project, "sbt_common/config/monitoring",
  localDependencies = Seq(config_core),
  externalDependencies = Dependencies.typesafeMonitoringDependencies
)

lazy val config_messaging = setupProject(project, "sbt_common/config/messaging",
  localDependencies = Seq(config_core, config_monitoring, config_storage),
  externalDependencies = Dependencies.configMessagingDependencies
)

lazy val config_elasticsearch = setupProject(project, "sbt_common/config/elasticsearch",
  localDependencies = Seq(config_core, elasticsearch)
)

lazy val api = setupProject(project, "catalogue_api/api",
  localDependencies = Seq(common, internal_model, display, elasticsearch),
  externalDependencies = Dependencies.apiDependencies
)
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)

lazy val ingestor = setupProject(project, "catalogue_pipeline/ingestor",
  localDependencies = Seq(common, config_elasticsearch, config_messaging)
)
  .settings(Search.settings: _*)

lazy val transformer_miro = setupProject(project,
  folder = "catalogue_pipeline/transformer/transformer_miro",
  localDependencies = Seq(common, internal_model, config_messaging),
  externalDependencies = Dependencies.miroTransformerDependencies
)

lazy val transformer_sierra = setupProject(project,
  folder = "catalogue_pipeline/transformer/transformer_sierra",
  localDependencies = Seq(common, internal_model, config_messaging)
)

lazy val merger = setupProject(project, "catalogue_pipeline/merger",
  localDependencies = Seq(common, internal_model, config_messaging)
)

lazy val id_minter = setupProject(project, "catalogue_pipeline/id_minter",
  localDependencies = Seq(common, internal_model, config_messaging),
  externalDependencies = Dependencies.idminterDependencies
)

lazy val recorder = setupProject(project, "catalogue_pipeline/recorder",
  localDependencies = Seq(common, internal_model, config_messaging, config_storage)
)

lazy val matcher = setupProject(project, "catalogue_pipeline/matcher",
  localDependencies = Seq(common, internal_model, config_messaging, config_storage),
  externalDependencies = Dependencies.scalaGraphDependencies
)

lazy val reindex_worker = setupProject(project, "reindexer/reindex_worker",
  localDependencies = Seq(common, config_messaging, config_storage)
)

lazy val goobi_reader = setupProject(project, "goobi_adapter/goobi_reader",
  localDependencies = Seq(config_messaging, config_storage)
)

lazy val sierra_adapter_common = setupProject(project, "sierra_adapter/common",
  localDependencies = Seq(internal_model, config_messaging, config_storage)
)

lazy val sierra_reader = setupProject(project, "sierra_adapter/sierra_reader",
  localDependencies = Seq(common, sierra_adapter_common),
  externalDependencies = Dependencies.sierraReaderDependencies
)

lazy val sierra_items_to_dynamo = setupProject(project,
  folder = "sierra_adapter/sierra_items_to_dynamo",
  localDependencies = Seq(common, sierra_adapter_common)
)

lazy val sierra_bib_merger = setupProject(project, "sierra_adapter/sierra_bib_merger",
  localDependencies = Seq(common, sierra_adapter_common)
)

lazy val sierra_item_merger = setupProject(project, "sierra_adapter/sierra_item_merger",
  localDependencies = Seq(common, sierra_adapter_common)
)

lazy val snapshot_generator = setupProject(project, "data_api/snapshot_generator",
  localDependencies = Seq(
    common, internal_model, display, config_elasticsearch, config_messaging
  ),
  externalDependencies = Dependencies.snapshotGeneratorDependencies
)

lazy val storage_common = setupProject(project, "storage/common",
  localDependencies = Seq(common, config_messaging, config_storage),
  externalDependencies = Dependencies.storageCommonDependencies
)

lazy val storage_display = setupProject(project, "storage/display",
  localDependencies = Seq(storage_common)
)

lazy val archivist = setupProject(project, "storage/archivist",
  localDependencies = Seq(storage_common)
)

lazy val notifier = setupProject(project, "storage/notifier",
  localDependencies = Seq(storage_common, storage_display),
  externalDependencies = Dependencies.wiremockDependencies
)

lazy val bags_common = setupProject(project, "storage/bags_common",
  localDependencies = Seq(storage_common)
)

lazy val bags = setupProject(project, "storage/bags",
  localDependencies = Seq(bags_common)
)

lazy val bags_api = setupProject(project, "storage/bags_api",
  localDependencies = Seq(bags_common, storage_display),
  externalDependencies = Dependencies.bagsApiDependencies
)

lazy val ingests_common = setupProject(project, "storage/ingests_common",
  localDependencies = Seq(storage_common)
)

lazy val ingests = setupProject(project, "storage/ingests",
  localDependencies = Seq(ingests_common),
  externalDependencies = Dependencies.wiremockDependencies
)

lazy val ingests_api = setupProject(project, "storage/ingests_api",
  localDependencies = Seq(ingests_common, storage_display),
  externalDependencies = Dependencies.ingestsApiDependencies
)

lazy val bag_replicator = setupProject(project, "storage/bag_replicator",
  localDependencies = Seq(storage_common)
)

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
