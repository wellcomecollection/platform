package uk.ac.wellcome.storage.s3

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

object S3ClientFactory {
  def create(region: String,
             endpoint: String,
             accessKey: String,
             secretKey: String): AmazonS3 = {
    val standardClient = AmazonS3ClientBuilder.standard
    if (endpoint.isEmpty)
      standardClient
        .withRegion(region)
        .build()
    else
      standardClient
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)))
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
        .build()
  }
}
