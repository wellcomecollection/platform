package uk.ac.wellcome.platform.idminter

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Json
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.MessagingBuilder
import uk.ac.wellcome.platform.idminter.config.builders.{
  IdentifiersTableBuilder,
  RDSBuilder
}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.IdentifiersTable
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService
import uk.ac.wellcome.platform.idminter.steps.{IdEmbedder, IdentifierGenerator}

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val identifiersTableConfig = IdentifiersTableBuilder.buildConfig(config)

  val identifierGenerator = new IdentifierGenerator(
    identifiersDao = new IdentifiersDao(
      db = RDSBuilder.buildDB(config),
      identifiers = new IdentifiersTable(
        identifiersTableConfig = identifiersTableConfig
      )
    )
  )

  val idEmbedder = new IdEmbedder(
    identifierGenerator = identifierGenerator
  )

  val workerService = new IdMinterWorkerService(
    idEmbedder = idEmbedder,
    writer = MessagingBuilder.buildMessageWriter[Json](config),
    messageStream = MessagingBuilder.buildMessageStream[Json](config),
    rdsClientConfig = RDSBuilder.buildRDSClientConfig(config),
    identifiersTableConfig = identifiersTableConfig
  )

  run(workerService)
}
