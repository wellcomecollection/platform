package uk.ac.wellcome.finatra.monitoring

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.AWSConfigModule
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.monitoring.CloudWatchClientFactory

object CloudWatchClientModule extends TwitterModule {
  override val modules = Seq(AWSConfigModule)

  private val endpoint = flag[String](
    "aws.cloudWatch.endpoint",
    "",
    "Endpoint of AWS CloudWatch. If not set, it will use the region")

  @Provides
  @Singleton
  def providesAmazonCloudWatch(awsConfig: AWSConfig): AmazonCloudWatch =
    CloudWatchClientFactory.create(
      region = awsConfig.region,
      endpoint = endpoint()
    )
}
