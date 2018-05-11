package uk.ac.wellcome.finatra.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync, AmazonSQSAsyncClientBuilder, AmazonSQSClientBuilder}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.models.aws.AWSConfig

object SQSClientModule extends TwitterModule {
  val sqsEndpoint = flag[String](
    "aws.sqs.endpoint",
    "",
    "Endpoint for AWS SQS Service. If not provided, the region will be used instead")

  private val accessKey =
    flag[String]("aws.sqs.accessKey", "", "AccessKey to access SQS")
  private val secretKey =
    flag[String]("aws.sqs.secretKey", "", "SecretKey to access SQS")

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
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey(), secretKey())))
        .build()
  }

  @Singleton
  @Provides
  def providesSQSAsyncClient(awsConfig: AWSConfig): AmazonSQSAsync = {
    val sqsClientBuilder = AmazonSQSAsyncClientBuilder.standard
    if (sqsEndpoint().isEmpty)
      sqsClientBuilder
        .withRegion(awsConfig.region)
        .build()
    else
      sqsClientBuilder
        .withEndpointConfiguration(
          new EndpointConfiguration(sqsEndpoint(), awsConfig.region))
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey(), secretKey())))
        .build()
  }

}
