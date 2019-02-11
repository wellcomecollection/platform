import java.io.File
import _root_.io.circe.syntax._
import _root_.io.circe.generic.auto._
import _root_.sbt.IO

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
  val dependencyIds: List[String] = localDependencies
    .map { p: Project => p.id }
    .toList

  case class ProjectMetadata(
                            id: String,
                            folder: String,
                            dependencyIds: List[String]
                            )

  val metadata = ProjectMetadata(
    id = project.id,
    folder = folder,
    dependencyIds = dependencyIds
  )

  IO.write(file, metadata.asJson.spaces2)

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

lazy val internal_model = setupProject(project, "sbt_common/internal_model",
  externalDependencies = Dependencies.internalModelDependencies
)

lazy val display = setupProject(project, "sbt_common/display",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.commonDisplayDependencies
)

lazy val elasticsearch = setupProject(project, "sbt_common/elasticsearch",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.commonElasticsearchDependencies
)

lazy val config_elasticsearch = setupProject(project, "sbt_common/config/elasticsearch",
  localDependencies = Seq(elasticsearch),
  externalDependencies = WellcomeDependencies.typesafeLibrary
)

lazy val api = setupProject(project, "catalogue_api/api",
  localDependencies = Seq(internal_model, display, elasticsearch),
  externalDependencies = Dependencies.apiDependencies
)
  .settings(Search.settings: _*)
  .settings(Swagger.settings: _*)

lazy val ingestor = setupProject(project, "catalogue_pipeline/ingestor",
  localDependencies = Seq(config_elasticsearch),
  externalDependencies = Dependencies.ingestorDependencies
)
  .settings(Search.settings: _*)

lazy val transformer_miro = setupProject(project,
  folder = "catalogue_pipeline/transformer/transformer_miro",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.miroTransformerDependencies
)

lazy val transformer_sierra = setupProject(project,
  folder = "catalogue_pipeline/transformer/transformer_sierra",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.sierraTransformerDependencies
)

lazy val merger = setupProject(project, "catalogue_pipeline/merger",
  localDependencies = Seq(internal_model),
  externalDependencies = WellcomeDependencies.messagingTypesafeLibrary
)

lazy val id_minter = setupProject(project, "catalogue_pipeline/id_minter",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.idminterDependencies ++ WellcomeDependencies.messagingTypesafeLibrary
)

lazy val recorder = setupProject(project, "catalogue_pipeline/recorder",
  localDependencies = Seq(internal_model),
  externalDependencies = WellcomeDependencies.messagingTypesafeLibrary
)

lazy val matcher = setupProject(project, "catalogue_pipeline/matcher",
  localDependencies = Seq(internal_model),
  externalDependencies = Dependencies.scalaGraphDependencies ++ WellcomeDependencies.messagingTypesafeLibrary
)

lazy val reindex_worker = setupProject(project, "reindexer/reindex_worker",
  externalDependencies = WellcomeDependencies.messagingTypesafeLibrary
)

lazy val goobi_reader = setupProject(project, "goobi_adapter/goobi_reader",
  externalDependencies = Dependencies.goobiReaderDependencies ++ WellcomeDependencies.messagingTypesafeLibrary
)

lazy val sierra_adapter_common = setupProject(project, "sierra_adapter/common",
  localDependencies = Seq(internal_model),
  externalDependencies = WellcomeDependencies.messagingTypesafeLibrary
)

lazy val sierra_reader = setupProject(project, "sierra_adapter/sierra_reader",
  localDependencies = Seq(sierra_adapter_common),
  externalDependencies = Dependencies.sierraReaderDependencies
)

lazy val sierra_items_to_dynamo = setupProject(project,
  folder = "sierra_adapter/sierra_items_to_dynamo",
  localDependencies = Seq(sierra_adapter_common)
)

lazy val sierra_bib_merger = setupProject(project, "sierra_adapter/sierra_bib_merger",
  localDependencies = Seq(sierra_adapter_common)
)

lazy val sierra_item_merger = setupProject(project, "sierra_adapter/sierra_item_merger",
  localDependencies = Seq(sierra_adapter_common)
)

lazy val snapshot_generator = setupProject(project, "data_api/snapshot_generator",
  localDependencies = Seq(internal_model, display, config_elasticsearch),
  externalDependencies = Dependencies.snapshotGeneratorDependencies ++ WellcomeDependencies.messagingTypesafeLibrary
)

lazy val root = (project in file("."))
  .aggregate(
    internal_model,
    display,
    elasticsearch,

    config_elasticsearch,

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
    snapshot_generator
  )
