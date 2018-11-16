package uk.ac.wellcome.platform.sierra_reader

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SQSBuilder
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.sierra_reader.config.builders.{ReaderConfigBuilder, SierraAPIConfigBuilder}
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()

  val metricsSender = MetricsBuilder.buildMetricsSender(config)

  val sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)

  val workerService = new SierraReaderWorkerService(
    sqsStream = sqsStream,
    s3client = S3Builder.buildS3Client(config),
    s3Config = S3Builder.buildS3Config(config),
    readerConfig = ReaderConfigBuilder.buildReaderConfig(config),
    sierraAPIConfig = SierraAPIConfigBuilder.buildSierraConfig(config)
  )

  run(workerService)
}
