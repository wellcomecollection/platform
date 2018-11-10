package uk.ac.wellcome.platform.sierra_bib_merger

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.sierra_bib_merger.services.{
  SierraBibMergerUpdaterService,
  SierraBibMergerWorkerService
}
import uk.ac.wellcome.sierra_adapter.config.builders.SierraTransformableVHSBuilder

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val versionedHybridStore =
    SierraTransformableVHSBuilder.buildSierraVHS(config)

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
