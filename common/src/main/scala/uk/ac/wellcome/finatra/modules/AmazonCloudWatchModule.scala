package uk.ac.wellcome.finatra.modules

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.twitter.app.Flaggable
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

import scala.concurrent.duration._

object AmazonCloudWatchModule extends TwitterModule {
  implicit val finteDurationFlaggable = Flaggable.mandatory[FiniteDuration](config => Duration.apply(config).asInstanceOf[FiniteDuration])

  flag[String](
    "aws.metrics.namespace",
    "",
    "Namespace for cloudwatch metrics")

  flag[FiniteDuration](
    "aws.metrics.flushInterval",
    10 minutes,
    "Interval within which metrics get flushed to cloudwatch. A short interval will result in an increased number of PutMetric requests.")

  private val awsEndpoint = flag[String](
    "aws.cloudWatch.endpoint",
    "",
    "Endpoint of AWS CloudWatch. If not set, it will use the region")

  @Provides
  @Singleton
  def providesAmazonCloudWatch(awsConfig: AWSConfig): AmazonCloudWatch = {
    val standardCloudWatchClient = AmazonCloudWatchClientBuilder.standard
    if (awsEndpoint().isEmpty) {
      standardCloudWatchClient
        .withRegion(awsConfig.region)
        .build()
    } else
      standardCloudWatchClient
        .withEndpointConfiguration(
          new EndpointConfiguration(awsEndpoint(), awsConfig.region))
        .build()
  }
}
