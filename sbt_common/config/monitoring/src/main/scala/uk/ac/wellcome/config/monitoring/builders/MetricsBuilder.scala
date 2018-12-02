package uk.ac.wellcome.config.monitoring.builders

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.monitoring.{MetricsConfig, MetricsSender}

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
    metricsConfig: MetricsConfig
  )(implicit actorSystem: ActorSystem): MetricsSender =
    new MetricsSender(
      amazonCloudWatch = cloudWatchClient,
      actorSystem = actorSystem,
      metricsConfig = metricsConfig
    )

  def buildMetricsSender(config: Config)(implicit actorSystem: ActorSystem): MetricsSender =
    buildMetricsSender(
      cloudWatchClient = CloudWatchBuilder.buildCloudWatchClient(config),
      metricsConfig = buildMetricsConfig(config)
    )
}
