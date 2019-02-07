package uk.ac.wellcome.platform.storage.ingests.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.WellcomeTypesafeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.DynamoBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.HTTPServerBuilder
import uk.ac.wellcome.platform.storage.ingests.api.http.HttpMetrics

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val httpMetrics = new HttpMetrics(
      metricsSender = MetricsBuilder.buildMetricsSender(config)
    )

    new IngestsApi(
      dynamoClient = DynamoBuilder.buildDynamoClient(config),
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config),
      snsWriter = SNSBuilder.buildSNSWriter(config),
      httpMetrics = httpMetrics,
      httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
      contextURL = HTTPServerBuilder.buildContextURL(config)
    )
  }
}
