package uk.ac.wellcome.platform.sierra_item_merger

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.services.{
  SierraItemMergerUpdaterService,
  SierraItemMergerWorkerService
}
import uk.ac.wellcome.sierra_adapter.config.builders.SierraTransformableVHSBuilder

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val versionedHybridStore =
    SierraTransformableVHSBuilder.buildSierraVHS(config)

  val updaterService = new SierraItemMergerUpdaterService(
    versionedHybridStore = versionedHybridStore
  )

  val workerService = new SierraItemMergerWorkerService(
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
    sierraItemMergerUpdaterService = updaterService,
    objectStore = S3Builder.buildObjectStore[SierraItemRecord](config),
    snsWriter = SNSBuilder.buildSNSWriter(config)
  )

  try {
    info(s"Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
