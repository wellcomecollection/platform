package uk.ac.wellcome.platform.sierra_bib_merger

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.sierra_bib_merger.services.{SierraBibMergerUpdaterService, SierraBibMergerWorkerService}
import uk.ac.wellcome.sierra_adapter.config.builders.SierraTransformableVHSBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val versionedHybridStore =
    SierraTransformableVHSBuilder.buildSierraVHS(config)

  val updaterService = new SierraBibMergerUpdaterService(
    versionedHybridStore = versionedHybridStore
  )

  val workerService = new SierraBibMergerWorkerService(
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
    snsWriter = SNSBuilder.buildSNSWriter(config),
    sierraBibMergerUpdaterService = updaterService
  )

  run(workerService)
}
