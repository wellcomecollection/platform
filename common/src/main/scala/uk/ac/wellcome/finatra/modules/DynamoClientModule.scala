package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig
import com.amazonaws.services.dynamodbv2._

object DynamoClientModule extends TwitterModule {
  override val modules = Seq(AWSConfigModule)
  private val dynamoDbEndpoint = flag[String](
    "aws.dynamoDb.endpoint",
    "",
    "Endpoint of AWS DynamoDB. if not present it will use the region")
  private val accessKey = flag[String](
    "aws.dynamoDb.accessKey",
    "",
    "AccessKey to access DynamoDB")
  private val secretKey = flag[String](
    "aws.dynamoDb.secretKey",
    "",
    "SecretKey to access DynamoDB")

  @Singleton
  @Provides
  def providesDynamoClient(awsConfig: AWSConfig): AmazonDynamoDB = {
    val standardDynamoDBClientBuilder = AmazonDynamoDBClientBuilder.standard
    createAwsClient(awsConfig, standardDynamoDBClientBuilder)
  }

  @Singleton
  @Provides
  def providesDynamoStreamsClient(
    awsConfig: AWSConfig): AmazonDynamoDBStreams = {
    val standardDynamoStreamsClientBuilder = AmazonDynamoDBStreamsClientBuilder
      .standard()
    createAwsClient(awsConfig, standardDynamoStreamsClientBuilder)
  }

  @Singleton
  @Provides
  def providesDynamoAsyncClient(awsConfig: AWSConfig): AmazonDynamoDBAsync = {
    val standardDynamoDbAsyncClientBuilder = AmazonDynamoDBAsyncClientBuilder
      .standard()
    createAwsClient(awsConfig, standardDynamoDbAsyncClientBuilder)
  }

  private def createAwsClient[T <: AwsClientBuilder[_, _], J](
    awsConfig: AWSConfig,
    awsClientBuilder: AwsClientBuilder[T, J]): J = {
    if (dynamoDbEndpoint().isEmpty) {
      awsClientBuilder
        .setRegion(awsConfig.region)
      awsClientBuilder.build()
    } else {
      awsClientBuilder
        .setEndpointConfiguration(
          new EndpointConfiguration(dynamoDbEndpoint(), awsConfig.region))
      awsClientBuilder.withCredentials(
        new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey(),
                                  secretKey())))
      awsClientBuilder.build()
    }
  }
}
