package uk.ac.wellcome.monitoring

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{Provides, Singleton}
import com.twitter.app.Flaggable
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.AkkaModule

import scala.concurrent.duration._

object MetricsSenderModule extends TwitterModule {
  override val modules = Seq(AkkaModule, CloudWatchClientModule)

  implicit val finoteDurationFlaggable =
    Flaggable.mandatory[FiniteDuration](config =>
      Duration.apply(config).asInstanceOf[FiniteDuration])

  val flushInterval = flag[FiniteDuration](
    "aws.metrics.flushInterval",
    10 minutes,
    "Interval within which metrics get flushed to cloudwatch. A short interval will result in an increased number of PutMetric requests."
  )

  @Provides
  @Singleton
  def providesMetricsSender(amazonCloudWatch: AmazonCloudWatch,
                            actorSystem: ActorSystem) =
    new MetricsSender(
      flushInterval = flushInterval(),
      amazonCloudWatch = amazonCloudWatch,
      actorSystem = actorSystem
    )
}
