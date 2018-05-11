package uk.ac.wellcome.monitoring

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.AkkaModule

object MetricsSenderModule extends TwitterModule {
  override val modules = Seq(AkkaModule, CloudWatchClientModule)

  @Provides
  @Singleton
  def providesMetricsSender(amazonCloudWatch: AmazonCloudWatch,
                            actorSystem: ActorSystem) =
    new MetricsSender(
      amazonCloudWatch = amazonCloudWatch,
      actorSystem = actorSystem
    )
}
