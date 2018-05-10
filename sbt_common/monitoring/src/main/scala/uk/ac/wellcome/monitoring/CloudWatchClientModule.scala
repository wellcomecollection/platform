package uk.ac.wellcome.monitoring

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.AWSConfigModule
import uk.ac.wellcome.models.aws.AWSConfig

object CloudWatchClientModule extends TwitterModule {
  override val modules = Seq(AWSConfigModule)

  private val endpoint = flag[String](
    "aws.cloudWatch.endpoint",
    "",
    "Endpoint of AWS CloudWatch. If not set, it will use the region")

  @Provides
  @Singleton
  def providesAmazonCloudWatch(awsConfig: AWSConfig): AmazonCloudWatch =
    buildCloudWatchClient(
      awsConfig = awsConfig,
      endpoint = endpoint()
    )

  def buildCloudWatchClient(awsConfig: AWSConfig, endpoint: String): AmazonCloudWatch = {
    val standardClient = AmazonCloudWatchClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardClient
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, awsConfig.region))
        .build()
  }
}
