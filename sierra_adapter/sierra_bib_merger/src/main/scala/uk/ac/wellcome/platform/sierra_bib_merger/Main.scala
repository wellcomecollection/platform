package uk.ac.wellcome.platform.sierra_bib_merger

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.{DynamoBuilder, S3Builder, VHSBuilder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.platform.sierra_bib_merger.services.{SierraBibMergerUpdaterService, SierraBibMergerWorkerService}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext = AkkaBuilder.buildExecutionContext()

  implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
    s3Client = S3Builder.buildS3Client(config)
  )

  val versionedHybridStore = new VersionedHybridStore[
    SierraTransformable,
    EmptyMetadata,
    ObjectStore[SierraTransformable]](
    vhsConfig = VHSBuilder.buildVHSConfig(config),
    objectStore = ObjectStore[SierraTransformable],
    dynamoDbClient = DynamoBuilder.buildDynamoClient(config)
  )

  val updaterService = new SierraBibMergerUpdaterService(
    versionedHybridStore = versionedHybridStore
  )

  val sqsStream = new SQSStream[NotificationMessage](
    actorSystem = actorSystem,
    sqsClient = SQSBuilder.buildSQSAsyncClient(config),
    sqsConfig = SQSBuilder.buildSQSConfig(config),
    metricsSender = MetricsBuilder.buildMetricsSender(config)
  )

  val workerService = new SierraBibMergerWorkerService(
    system = actorSystem,
    sqsStream = sqsStream,
    snsWriter = SNSBuilder.buildSNSWriter(config),
    sierraBibMergerUpdaterService = updaterService
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
