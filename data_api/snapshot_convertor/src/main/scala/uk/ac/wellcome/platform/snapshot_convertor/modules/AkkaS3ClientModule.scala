package uk.ac.wellcome.platform.snapshot_convertor.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.AwsRegionProvider
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.{AWSConfig, S3Config}

object AkkaS3ClientModule extends TwitterModule {
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
  def providesAkkaS3Client(awsConfig: AWSConfig,
                           s3Config: S3Config,
                           actorSystem: ActorSystem): S3Client = {
    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = awsConfig.region
      }

    val credentialsProvider = if (s3Config.endpoint.isEmpty) {
      DefaultAWSCredentialsProviderChain.getInstance()
    } else {
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(s3Config.accessKey, s3Config.secretKey)
      )
    }

    val actorMaterializer = ActorMaterializer()(actorSystem)
    val endpointUrl = if (s3Config.endpoint.isEmpty) {
      None
    } else {
      Some(s3Config.endpoint)
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
