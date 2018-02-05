package uk.ac.wellcome.finatra.modules

import akka.actor.ActorSystem
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.AWSConfig

object AmazonCloudWatchModule extends TwitterModule {
  private val awsNamespace = flag[String]("aws.metrics.namespace",
                                          "",
                                          "Namespace for cloudwatch metrics")
  private val awsEndpoint = flag[String](
    "aws.cloudWatch.endpoint",
    "",
    "Endpoint of AWS CloudWatch. If not set, it will use the region")

  @Provides
  @Singleton
  def providesMetricsSender(amazonCloudWatch: AmazonCloudWatch, actorSystem: ActorSystem) =
    new MetricsSender(awsNamespace(), amazonCloudWatch, actorSystem)

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
