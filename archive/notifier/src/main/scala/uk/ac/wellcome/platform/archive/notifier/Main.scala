package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import akka.Done
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.platform.archive.common.WellcomeApp
import uk.ac.wellcome.platform.archive.common.config.builders.EnrichConfig._
import uk.ac.wellcome.platform.archive.common.config.builders.{
  AkkaBuilder,
  MetricsBuilder,
  SNSBuilder,
  SQSBuilder
}

import scala.concurrent.Future

object Main extends WellcomeApp {
  val config = ConfigFactory.load()

  implicit val actorSystem = AkkaBuilder.buildActorSystem()
  implicit val materializer = AkkaBuilder.buildActorMaterializer()

  val notifier = new Notifier(
    sqsClient = SQSBuilder.buildSQSAsyncClient(config),
    sqsConfig = SQSBuilder.buildSQSConfig(config),
    snsClient = SNSBuilder.buildSNSClient(config),
    snsConfig = SNSBuilder.buildSNSConfig(config),
    metricsSender = MetricsBuilder.buildMetricsSender(config),
    contextUrl = buildContextURL(config)
  )

  def run(): Future[Done] =
    notifier.run()

  private def buildContextURL(config: Config): URL =
    new URL(config.required[String]("notifier.context-url"))
}
