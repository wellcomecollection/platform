package uk.ac.wellcome.transformer.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

object AmazonKinesisModule extends TwitterModule {
  private val dynamoDbStreamsEndpoint = flag[String](
    "aws.dynamoDb.streams.endpoint",
    "",
    "Endpoint of AWS DynamoDB streams. if not present it will use the region")

  @Provides
  @Singleton
  def providesAmazonKinesis(awsConfig: AWSConfig): AmazonKinesis = {
    if (dynamoDbStreamsEndpoint().isEmpty) {
      val adapter = new AmazonDynamoDBStreamsAdapterClient(
        new DefaultAWSCredentialsProviderChain()
      )
      adapter.setRegion(RegionUtils.getRegion(awsConfig.region))
      adapter
    } else {
      new AmazonDynamoDBStreamsAdapterClient(
        AmazonDynamoDBStreamsClientBuilder
          .standard()
          .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(awsConfig.accessKey.get,
                                    awsConfig.secretKey.get)))
          .withEndpointConfiguration(
            new EndpointConfiguration(dynamoDbStreamsEndpoint(), awsConfig.region))
          .build())
    }

  }
}
