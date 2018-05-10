package uk.ac.wellcome.storage.dynamo

import javax.inject.Singleton

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.finatra.modules.AWSConfigModule
import com.amazonaws.services.dynamodbv2._

object DynamoClientModule extends TwitterModule {
  override val modules = Seq(AWSConfigModule)
  private val dynamoDbEndpoint = flag[String](
    "aws.dynamoDb.endpoint",
    "",
    "Endpoint of AWS DynamoDB. if not present it will use the region")
  private val accessKey =
    flag[String]("aws.dynamoDb.accessKey", "", "AccessKey to access DynamoDB")
  private val secretKey =
    flag[String]("aws.dynamoDb.secretKey", "", "SecretKey to access DynamoDB")

  @Singleton
  @Provides
  def providesDynamoClient(awsConfig: AWSConfig): AmazonDynamoDB =
    buildDynamoClient(
      awsConfig = awsConfig,
      endpoint = dynamoDbEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  def buildDynamoClient(awsConfig: AWSConfig, endpoint: String, accessKey: String, secretKey: String): AmazonDynamoDB = {
    val standardClient = AmazonDynamoDBClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardClient
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, awsConfig.region))
        .build()
  }
}
