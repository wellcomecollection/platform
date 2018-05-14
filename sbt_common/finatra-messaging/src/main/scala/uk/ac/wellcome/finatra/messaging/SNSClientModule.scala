package uk.ac.wellcome.finatra.messaging

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.models.aws.AWSConfig

object SNSClientModule extends TwitterModule {
  val snsEndpoint = flag[String](
    "aws.sns.endpoint",
    "",
    "Endpoint of AWS SNS. The region will be used if the enpoint is not provided")

  private val accessKey =
    flag[String]("aws.sns.accessKey", "", "AccessKey to access SNS")
  private val secretKey =
    flag[String]("aws.sns.secretKey", "", "SecretKey to access SNS")

  @Singleton
  @Provides
  def providesSNSClient(awsConfig: AWSConfig): AmazonSNS =
    buildSNSClient(
      awsConfig = awsConfig,
      endpoint = snsEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  def buildSNSClient(awsConfig: AWSConfig,
                     endpoint: String,
                     accessKey: String,
                     secretKey: String): AmazonSNS = {
    val standardClient = AmazonSNSClientBuilder.standard
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
