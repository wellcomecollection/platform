package uk.ac.wellcome.platform.snapshot_generator

import akka.actor.ActorSystem
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.snapshot_generator.config.builders.AkkaS3Builder
import uk.ac.wellcome.platform.snapshot_generator.services.{SnapshotGeneratorWorkerService, SnapshotService}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext = AkkaBuilder.buildExecutionContext()

  val snapshotService = new SnapshotService(
    actorSystem = actorSystem,
    akkaS3Client = AkkaS3Builder.buildAkkaS3Client(config),
    elasticClient = ElasticBuilder.buildHttpClient(config),
    elasticConfig = ElasticBuilder.buildElasticConfig(config),
    objectMapper = new ObjectMapper()
  )

  val sqsStream = new SQSStream[NotificationMessage](
    actorSystem = actorSystem,
    sqsClient = SQSBuilder.buildSQSClient(config),
    sqsConfig = SQSBuilder.buildSQSConfig(config),
    metricsSender = MetricsBuilder.buildMetricsSender(config)
  )

  val workerService = new SnapshotGeneratorWorkerService(
    snapshotService = snapshotService,
    sqsStream = sqsStream,
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
