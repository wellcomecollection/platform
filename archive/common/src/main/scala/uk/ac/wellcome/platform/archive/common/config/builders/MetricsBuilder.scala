package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.{MetricsConfig, MetricsSender}
import EnrichConfig._
import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import uk.ac.wellcome.config.core.builders.AkkaBuilder

import scala.concurrent.duration._

object MetricsBuilder {
  def buildMetricsConfig(config: Config): MetricsConfig = {
    val namespace =
      config.getOrElse[String]("aws.metrics.namespace")(default = "")
    val flushInterval = config.getOrElse[FiniteDuration](
      "aws.metrics.flushInterval")(default = 10 minutes)

    MetricsConfig(
      namespace = namespace,
      flushInterval = flushInterval
    )
  }

  private def buildMetricsSender(
    cloudWatchClient: AmazonCloudWatch,
    actorSystem: ActorSystem,
    metricsConfig: MetricsConfig
  ): MetricsSender =
    new MetricsSender(
      amazonCloudWatch = cloudWatchClient,
      actorSystem = actorSystem,
      metricsConfig = metricsConfig
    )

  def buildMetricsSender(config: Config): MetricsSender =
    buildMetricsSender(
      cloudWatchClient = CloudWatchBuilder.buildCloudWatchClient(config),
      actorSystem = AkkaBuilder.buildActorSystem(),
      metricsConfig = buildMetricsConfig(config)
    )
}
