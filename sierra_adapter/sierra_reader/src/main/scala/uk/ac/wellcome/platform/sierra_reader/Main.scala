package uk.ac.wellcome.platform.sierra_reader

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SQSBuilder
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.sierra_reader.config.builders.{ReaderConfigBuilder, SierraAPIConfigBuilder}
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()

  val metricsSender = MetricsBuilder.buildMetricsSender(config)

  val sqsStream = new SQSStream[NotificationMessage](
    actorSystem = actorSystem,
    sqsClient = SQSBuilder.buildSQSAsyncClient(config),
    sqsConfig = SQSBuilder.buildSQSConfig(config),
    metricsSender = metricsSender
  )

  val workerService = new SierraReaderWorkerService(
    actorSystem = actorSystem,
    sqsStream = sqsStream,
    s3client = S3Builder.buildS3Client(config),
    s3Config = S3Builder.buildS3Config(config),
    readerConfig = ReaderConfigBuilder.buildReaderConfig(config),
    sierraAPIConfig = SierraAPIConfigBuilder.buildSierraConfig(config)
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
