package uk.ac.wellcome.platform.sierra_items_to_dynamo

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.VHSBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.{
  DynamoInserter,
  SierraItemsToDynamoWorkerService
}
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val versionedHybridStore = VHSBuilder.buildVHS[SierraItemRecord, EmptyMetadata](config)

  val dynamoInserter = new DynamoInserter(
    versionedHybridStore = versionedHybridStore
  )

  val workerService = new SierraItemsToDynamoWorkerService(
    actorSystem = actorSystem,
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
    dynamoInserter = dynamoInserter,
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
