package uk.ac.wellcome.platform.idminter

import akka.actor.ActorSystem
import com.typesafe.config.Config
import io.circe.Json
import uk.ac.wellcome.messaging.typesafe.MessagingBuilder
import uk.ac.wellcome.platform.idminter.config.builders.{
  IdentifiersTableBuilder,
  RDSBuilder
}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.IdentifiersTable
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService
import uk.ac.wellcome.platform.idminter.steps.{IdEmbedder, IdentifierGenerator}
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
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

    new IdMinterWorkerService(
      idEmbedder = idEmbedder,
      writer = MessagingBuilder.buildMessageWriter[Json](config),
      messageStream = MessagingBuilder.buildMessageStream[Json](config),
      rdsClientConfig = RDSBuilder.buildRDSClientConfig(config),
      identifiersTableConfig = identifiersTableConfig
    )
  }
}
