package uk.ac.wellcome.storage.s3

import javax.inject.Singleton

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.storage.s3.S3Config

object S3ClientModule extends TwitterModule {

  @Singleton
  @Provides
  def providesS3Client(awsConfig: AWSConfig, s3Config: S3Config): AmazonS3 = {
    val standardClient = AmazonS3ClientBuilder.standard
    if (s3Config.endpoint.isEmpty)
      standardClient
        .withRegion(awsConfig.region)
        .build()
    else
      standardClient
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(s3Config.accessKey, s3Config.secretKey)))
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(
          new EndpointConfiguration(s3Config.endpoint, awsConfig.region))
        .build()
  }

}
