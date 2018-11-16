package uk.ac.wellcome.platform.ingestor

import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.elasticsearch.builders.ElasticBuilder
import uk.ac.wellcome.config.messaging.builders.MessagingBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.config.builders.IngestorConfigBuilder
import uk.ac.wellcome.platform.ingestor.services.IngestorWorkerService

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val workerService = new IngestorWorkerService(
    elasticClient = ElasticBuilder.buildHttpClient(config),
    ingestorConfig = IngestorConfigBuilder.buildIngestorConfig(config),
    messageStream =
      MessagingBuilder.buildMessageStream[IdentifiedBaseWork](config)
  )

  run(workerService)
}
