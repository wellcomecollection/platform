package uk.ac.wellcome.platform.archiver.modules

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.AwsRegionProvider
import com.google.inject.{AbstractModule, Provides, Singleton}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.AppConfig


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
      actorSystem = actorSystem,
      region = appConfig.awsS3Region.toOption.get,
      endpoint = appConfig.awsS3Region.toOption.get,
      accessKey = appConfig.awsS3AccessKey.toOption.get,
      secretKey = appConfig.awsS3SecretKey.toOption.get
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
