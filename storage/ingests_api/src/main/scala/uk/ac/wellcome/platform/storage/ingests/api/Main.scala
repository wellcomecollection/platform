package uk.ac.wellcome.platform.storage.ingests.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.typesafe.SNSBuilder
import uk.ac.wellcome.monitoring.typesafe.MetricsSenderBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.HTTPServerBuilder
import uk.ac.wellcome.platform.archive.common.http.HttpMetrics
import uk.ac.wellcome.storage.typesafe.DynamoBuilder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val httpMetrics = new HttpMetrics(
      name = "IngestsApi",
      metricsSender = MetricsSenderBuilder.buildMetricsSender(config)
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
