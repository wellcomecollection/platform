package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig
import com.amazonaws.services.sns._

object SNSClientModule extends TwitterModule {
  val snsEndpoint = flag[String](
    "aws.sns.endpoint",
    "",
    "Endpoint of AWS SNS. The region will be used if the enpoint is not provided")
  @Singleton
  @Provides
  def providesSNSClient(awsConfig: AWSConfig): AmazonSNS = {
    val standardSnsClient = AmazonSNSClientBuilder.standard
    if (snsEndpoint().isEmpty)
      standardSnsClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardSnsClient
        .withEndpointConfiguration(
          new EndpointConfiguration(snsEndpoint(), awsConfig.region))
        .build()
  }
}
