package uk.ac.wellcome.platform.archiver.modules

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.impl.ListBucketVersion2
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.AwsRegionProvider
import com.google.inject.{AbstractModule, Provides, Singleton}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.S3ClientConfig


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
      endpointUrl = endpointUrl,
      listBucketApiVersion = ListBucketVersion2
    )

  @Singleton
  @Provides
  def buildAkkaS3Client(clientConfig: S3ClientConfig,
                        actorSystem: ActorSystem): S3Client = {

    val regionProvider = new AwsRegionProvider {
      def getRegion: String = clientConfig.region
    }

    val explicitConfigSettings = for {
      endpoint <- clientConfig.endpoint
      accessKey <- clientConfig.accessKey
      secretKey <- clientConfig.secretKey
    } yield {
      val credentialsProvider = new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey)
      )

      akkaS3Settings(
        credentialsProvider = credentialsProvider,
        regionProvider = regionProvider,
        endpointUrl = Some(endpoint)
      )
    }

    val settings = explicitConfigSettings.getOrElse {
      val credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance()

      akkaS3Settings(
        credentialsProvider = credentialsProvider,
        regionProvider = regionProvider,
        endpointUrl = None
      )
    }

    val actorMaterializer = ActorMaterializer()(actorSystem)

    logger.debug(s"creating S3 Akka client with settings=[$settings]")

    new S3Client(settings)(actorSystem, actorMaterializer)
  }
}
