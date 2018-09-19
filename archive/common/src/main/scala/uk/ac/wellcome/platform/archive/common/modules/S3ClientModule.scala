package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.{AbstractModule, Provides, Singleton}

object S3ClientModule extends AbstractModule {

  @Singleton
  @Provides
  def buildS3Client(clientConfig: S3ClientConfig): AmazonS3 = {

    val standardClient = AmazonS3ClientBuilder.standard
    val region = clientConfig.region

    val explicitConfigSettings = for {
      endpoint <- clientConfig.endpoint
      accessKey <- clientConfig.accessKey
      secretKey <- clientConfig.secretKey
    } yield {
      val credentialsProvider = new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey)
      )

      (credentialsProvider, endpoint)
    }

    explicitConfigSettings match {

      case Some((credentialsProvider, endpoint)) => standardClient
        .withCredentials(credentialsProvider)
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
        .build()

      case None => standardClient
        .withRegion(region)
        .build()

    }
  }
}

