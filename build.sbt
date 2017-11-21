import sbt.Keys._

def doSharedSetup(project: Project) =
  project
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

lazy val reindexer = doSharedSetup(project)
  .settings(libraryDependencies ++= Dependencies.reindexerDependencies)

lazy val sierra_api = project
  .settings(Common.settings: _*)
  .settings(Revolver.settings: _*)
  .enablePlugins(JavaAppPackaging)
  .settings(libraryDependencies ++= Dependencies.sierraApiDependencies)

lazy val root = (project in file("."))
  .aggregate(common,
             api,
             ingestor,
             transformer,
             id_minter,
             reindexer, sierra_api)
