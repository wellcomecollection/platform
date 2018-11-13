package uk.ac.wellcome.platform.sierra_items_to_dynamo

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.{
  DynamoBuilder,
  S3Builder,
  VHSBuilder
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.{
  DynamoInserter,
  SierraItemsToDynamoWorkerService
}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
    s3Client = S3Builder.buildS3Client(config)
  )

  val versionedHybridStore = new VersionedHybridStore[
    SierraItemRecord,
    EmptyMetadata,
    ObjectStore[SierraItemRecord]](
    vhsConfig = VHSBuilder.buildVHSConfig(config),
    objectStore = ObjectStore[SierraItemRecord],
    dynamoDbClient = DynamoBuilder.buildDynamoClient(config)
  )

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
