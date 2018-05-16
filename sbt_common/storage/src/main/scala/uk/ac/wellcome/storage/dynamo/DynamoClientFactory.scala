package uk.ac.wellcome.storage.dynamo

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{
  AmazonDynamoDB,
  AmazonDynamoDBClientBuilder
}

object DynamoClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): AmazonDynamoDB = {
    val standardClient = AmazonDynamoDBClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(region)
        .build()
    else
      standardClient
        .withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(accessKey, secretKey)))
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
        .build()
  }
}
