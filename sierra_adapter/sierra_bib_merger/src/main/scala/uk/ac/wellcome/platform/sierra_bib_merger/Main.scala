package uk.ac.wellcome.platform.sierra_bib_merger

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.platform.sierra_bib_merger.services.{
  SierraBibMergerUpdaterService,
  SierraBibMergerWorkerService
}
import uk.ac.wellcome.sierra_adapter.config.builders.SierraTransformableVHSBuilder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val versionedHybridStore =
      SierraTransformableVHSBuilder.buildSierraVHS(config)

    val updaterService = new SierraBibMergerUpdaterService(
      versionedHybridStore = versionedHybridStore
    )

    new SierraBibMergerWorkerService(
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      snsWriter = SNSBuilder.buildSNSWriter(config),
      sierraBibMergerUpdaterService = updaterService
    )
  }
}
