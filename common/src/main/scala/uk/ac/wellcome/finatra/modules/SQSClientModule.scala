package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs._
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

object SQSClientModule extends TwitterModule {
  override val modules = Seq(SQSConfigModule)

  val sqsEndpoint = flag[String](
    "aws.sqs.endpoint",
    "",
    "Endpoint for AWS SQS Service. If not provided, the region will be used instead")

  @Singleton
  @Provides
  def providesSQSClient(awsConfig: AWSConfig): AmazonSQS = {
    val sqsClientBuilder = AmazonSQSClientBuilder.standard
    if (sqsEndpoint().isEmpty)
      sqsClientBuilder
        .withRegion(awsConfig.region)
        .build()
    else
      sqsClientBuilder
        .withEndpointConfiguration(
          new EndpointConfiguration(sqsEndpoint(), awsConfig.region))
        .withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(awsConfig.accessKey.get,
                                    awsConfig.secretKey.get)))
        .build()
  }

}
