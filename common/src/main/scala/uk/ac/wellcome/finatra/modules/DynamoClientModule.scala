package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
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

  @Singleton
  @Provides
  def providesDynamoClient(awsConfig: AWSConfig): AmazonDynamoDB = {
    val standardDynamoDBClientBuilder = AmazonDynamoDBClientBuilder.standard
    if (dynamoDbEndpoint().isEmpty)
      standardDynamoDBClientBuilder
        .withRegion(awsConfig.region)
        .build()
    else
      standardDynamoDBClientBuilder
        .withEndpointConfiguration(
          new EndpointConfiguration(dynamoDbEndpoint(), awsConfig.region))
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(awsConfig.accessKey.get, awsConfig.secretKey.get)))
        .build()
  }

  @Singleton
  @Provides
  def providesDynamoStreamsClient(awsConfig: AWSConfig): AmazonDynamoDBStreams = {
    val standardDynamoStreamsClientBuilder = AmazonDynamoDBStreamsClientBuilder
        .standard()
    if (dynamoDbEndpoint().isEmpty)
      standardDynamoStreamsClientBuilder
        .withRegion(awsConfig.region)
        .build()
    else
      standardDynamoStreamsClientBuilder
        .withEndpointConfiguration(
          new EndpointConfiguration(dynamoDbEndpoint(), awsConfig.region))
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(awsConfig.accessKey.get, awsConfig.secretKey.get)))
        .build()
  }

  @Singleton
  @Provides
  def providesDynamoAsyncClient(awsConfig: AWSConfig): AmazonDynamoDBAsync = {
    val standardDynamoDbAsyncClientBuilder = AmazonDynamoDBAsyncClientBuilder
      .standard()
    if (dynamoDbEndpoint().isEmpty)
      standardDynamoDbAsyncClientBuilder
        .withRegion(awsConfig.region)
        .build()
    else
      standardDynamoDbAsyncClientBuilder
        .withEndpointConfiguration(
          new EndpointConfiguration(dynamoDbEndpoint(), awsConfig.region))
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(awsConfig.accessKey.get, awsConfig.secretKey.get)))
        .build()
  }
}
