package uk.ac.wellcome.finatra.monitoring

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.akka.AkkaModule
import uk.ac.wellcome.monitoring.{MetricsConfig, MetricsSender}

import scala.concurrent.ExecutionContext

object MetricsSenderModule extends TwitterModule {
  override val modules = Seq(
    AkkaModule,
    CloudWatchClientModule,
    MetricsConfigModule
  )

  @Provides
  @Singleton
  def providesMetricsSender(amazonCloudWatch: AmazonCloudWatch,
                            actorSystem: ActorSystem,
                            metricsConfig: MetricsConfig, executionContext: ExecutionContext) =
    new MetricsSender(
      amazonCloudWatch = amazonCloudWatch,
      actorSystem = actorSystem,
      metricsConfig = metricsConfig
    )(executionContext)
}
