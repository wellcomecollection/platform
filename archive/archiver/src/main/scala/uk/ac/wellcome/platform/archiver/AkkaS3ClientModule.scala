package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.AwsRegionProvider
import com.google.inject.{AbstractModule, Provides, Singleton}
import grizzled.slf4j.Logging

case class AppConfig(
  awsS3AccessKey: String = "",
  awsS3SecretKey: String = "",
  awsS3Region: String = "eu-west-1",
  awsS3Endpoint: String = ""
)

object AkkaS3ClientModule extends AbstractModule with Logging {
  def akkaS3Settings(credentialsProvider: AWSCredentialsProvider,
                     regionProvider: AwsRegionProvider,
                     endpointUrl: Option[String]): S3Settings =
    new S3Settings(
      bufferType = MemoryBufferType,
      proxy = None,
      credentialsProvider = credentialsProvider,
      s3RegionProvider = regionProvider,
      pathStyleAccess = true,
      endpointUrl = endpointUrl
    )

  @Singleton
  @Provides
  def providesAkkaS3Client(actorSystem: ActorSystem, appConfig: AppConfig): S3Client =
    buildAkkaS3Client(
      region = appConfig.awsS3Region,
      actorSystem = actorSystem,
      endpoint = appConfig.awsS3Region,
      accessKey = appConfig.awsS3AccessKey,
      secretKey = appConfig.awsS3SecretKey
    )

  def buildAkkaS3Client(region: String,
                        actorSystem: ActorSystem,
                        endpoint: String,
                        accessKey: String,
                        secretKey: String): S3Client = {
    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = region
      }

    val credentialsProvider = if (endpoint.isEmpty) {
      DefaultAWSCredentialsProviderChain.getInstance()
    } else {
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey)
      )
    }

    val actorMaterializer = ActorMaterializer()(actorSystem)
    val endpointUrl = if (endpoint.isEmpty) {
      None
    } else {
      Some(endpoint)
    }

    val settings = akkaS3Settings(
      credentialsProvider = credentialsProvider,
      regionProvider = regionProvider,
      endpointUrl = endpointUrl
    )

    logger.debug(s"creating S3 Akka client with settings=[$settings]")
    new S3Client(settings)(actorSystem, actorMaterializer)
  }
}
