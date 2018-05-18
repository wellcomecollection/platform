package uk.ac.wellcome.finatra.monitoring

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.monitoring.CloudWatchClientFactory

object CloudWatchClientModule extends TwitterModule {
  private val endpoint = flag[String](
    "aws.cloudWatch.endpoint",
    "",
    "Endpoint of AWS CloudWatch. If not set, it will use the region")

  private val region = flag[String]("aws.cloudWatch.region", "eu-west-1")

  @Provides
  @Singleton
  def providesAmazonCloudWatch(): AmazonCloudWatch =
    CloudWatchClientFactory.create(
      region = region(),
      endpoint = endpoint()
    )
}
