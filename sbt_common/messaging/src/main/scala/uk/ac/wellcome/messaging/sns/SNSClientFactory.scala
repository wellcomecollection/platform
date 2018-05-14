package uk.ac.wellcome.messaging.sns

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}

object SNSClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): AmazonSNS = {
    val standardClient = AmazonSNSClientBuilder.standard
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
