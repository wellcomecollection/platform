package uk.ac.wellcome.platform.ingestor

import com.typesafe.config.Config
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
  def buildWorkerService(config: Config): IngestorWorkerService = {
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    new IngestorWorkerService(
      elasticClient = ElasticBuilder.buildHttpClient(config),
      ingestorConfig = IngestorConfigBuilder.buildIngestorConfig(config),
      messageStream =
        MessagingBuilder.buildMessageStream[IdentifiedBaseWork](config)
    )
  }

  run()
}
