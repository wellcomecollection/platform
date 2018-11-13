package uk.ac.wellcome.platform.ingestor

import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.elasticsearch.builders.ElasticBuilder
import uk.ac.wellcome.config.messaging.builders.MessagingBuilder
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.config.builders.IngestorConfigBuilder
import uk.ac.wellcome.platform.ingestor.services.{IngestorWorkerService, WorkIndexer}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val workerService = new IngestorWorkerService(
    ingestorConfig = IngestorConfigBuilder.buildIngestorConfig(config),
    identifiedWorkIndexer = new WorkIndexer(
      elasticClient = ElasticBuilder.buildHttpClient(config)
    ),
    messageStream = MessagingBuilder.buildMessageStream[IdentifiedBaseWork](config)
  )

  try {
    info("Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info("Terminating worker.")
  }
}
