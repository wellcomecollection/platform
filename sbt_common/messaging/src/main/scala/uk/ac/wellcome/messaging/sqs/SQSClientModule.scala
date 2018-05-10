package uk.ac.wellcome.messaging.sqs

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs._
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.models.aws.AWSConfig

object SQSClientModule extends TwitterModule {
  override val modules = Seq(SQSConfigModule)

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
  def providesSQSClient(awsConfig: AWSConfig): AmazonSQS =
    buildSQSClient(
      awsConfig = awsConfig,
      endpoint = sqsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  def buildSQSClient(awsConfig: AWSConfig,
                     endpoint: String,
                     accessKey: String,
                     secretKey: String): AmazonSQS = {
    val standardClient = AmazonSQSClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardClient
        .withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, awsConfig.region))
        .build()
  }

  @Singleton
  @Provides
  def providesSQSAsyncClient(awsConfig: AWSConfig): AmazonSQSAsync =
    buildSQSAsyncClient(
      awsConfig = awsConfig,
      endpoint = sqsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  def buildSQSAsyncClient(awsConfig: AWSConfig,
                          endpoint: String,
                          accessKey: String,
                          secretKey: String): AmazonSQSAsync = {
    val standardClient = AmazonSQSAsyncClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardClient
        .withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, awsConfig.region))
        .build()
  }
}
