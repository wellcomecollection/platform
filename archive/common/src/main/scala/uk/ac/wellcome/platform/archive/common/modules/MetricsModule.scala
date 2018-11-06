package uk.ac.wellcome.platform.archive.common.modules

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.{MetricsConfig, MetricsSender}
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig._

import scala.concurrent.duration._

object MetricsModule extends AbstractModule {
  install(AkkaModule)
  install(CloudWatchModule)
  install(TypesafeConfigModule)

  @Singleton
  @Provides
  def providesMetricsConfig(config: Config): MetricsConfig = {
    val namespace = config.getOrElse[String]("aws.metrics.namespace")(default = "")
    val flushInterval = config.getOrElse[FiniteDuration]("aws.metrics.flushInterval")(default = 10 minutes)

    MetricsConfig(
      namespace = namespace,
      flushInterval = flushInterval
    )
  }

  @Provides
  @Singleton
  def providesMetricsSender(amazonCloudWatch: AmazonCloudWatch,
                            actorSystem: ActorSystem,
                            metricsConfig: MetricsConfig) =
    new MetricsSender(
      amazonCloudWatch = amazonCloudWatch,
      actorSystem = actorSystem,
      metricsConfig = metricsConfig
    )
}
