package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig

object S3ClientModule extends AbstractModule {

  import EnrichConfig._

  @Singleton
  @Provides
  def providesS3ClientConfig(config: Config) = {
    val key = config
      .get[String]("aws.s3.key")

    val secret = config
      .get[String]("aws.s3.secret")

    val endpoint = config
      .get[String]("aws.s3.endpoint")

    val region = config
      .getOrElse[String]("aws.s3.region")("eu-west-1")

    S3ClientConfig(key, secret, endpoint, region)
  }

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

      case Some((credentialsProvider, endpoint)) =>
        standardClient
          .withCredentials(credentialsProvider)
          .withPathStyleAccessEnabled(true)
          .withEndpointConfiguration(
            new EndpointConfiguration(endpoint, region))
          .build()

      case None =>
        standardClient
          .withRegion(region)
          .build()

    }
  }
}

case class S3ClientConfig(
  accessKey: Option[String] = None,
  secretKey: Option[String] = None,
  endpoint: Option[String] = None,
  region: String
)
