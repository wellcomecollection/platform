package uk.ac.wellcome.finatra.monitoring

import com.google.inject.{Provides, Singleton}
import com.twitter.app.Flaggable
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.monitoring.MetricsConfig

import scala.concurrent.duration._

object MetricsConfigModule extends TwitterModule {
  implicit val finiteDurationFlaggable =
    Flaggable.mandatory[FiniteDuration](config =>
      Duration.apply(config).asInstanceOf[FiniteDuration])

  val awsNamespace = flag[String](
    "aws.metrics.namespace",
    "",
    "Namespace for cloudwatch metrics")

  val flushInterval = flag[FiniteDuration](
    "aws.metrics.flushInterval",
    10 minutes,
    "Interval within which metrics get flushed to cloudwatch. A short interval will result in an increased number of PutMetric requests."
  )

  @Singleton
  @Provides
  def providesMetricsConfig(): MetricsConfig =
    MetricsConfig(
      namespace = awsNamespace(),
      flushInterval = flushInterval()
    )
}
