package uk.ac.wellcome.messaging.sqs

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{
  AmazonSQS,
  AmazonSQSAsync,
  AmazonSQSAsyncClientBuilder,
  AmazonSQSClientBuilder
}

object SQSClientFactory {
  def createAsyncClient(region: String,
                        endpoint: String,
                        accessKey: String,
                        secretKey: String): AmazonSQSAsync = {
    val standardClient = AmazonSQSAsyncClientBuilder.standard
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

  def createSyncClient(region: String,
                       endpoint: String,
                       accessKey: String,
                       secretKey: String): AmazonSQS = {
    val standardClient = AmazonSQSClientBuilder.standard
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
