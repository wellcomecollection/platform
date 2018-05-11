package uk.ac.wellcome.platform.snapshot_generator.finatra.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.AwsRegionProvider
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

object AkkaS3ClientModule extends TwitterModule {
  private val endpoint = flag[String](
    "aws.s3.endpoint",
    "",
    "Endpoint of AWS S3. The region will be used if the endpoint is not provided")
  private val accessKey =
    flag[String]("aws.s3.accessKey", "", "AccessKey to access S3")
  private val secretKey =
    flag[String]("aws.s3.secretKey", "", "SecretKey to access S3")

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
                           actorSystem: ActorSystem): S3Client =
    buildAkkaS3Client(
      awsConfig = awsConfig,
      actorSystem = actorSystem,
      endpoint = endpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )

  def buildAkkaS3Client(awsConfig: AWSConfig,
                        actorSystem: ActorSystem,
                        endpoint: String,
                        accessKey: String,
                        secretKey: String): S3Client = {
    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = awsConfig.region
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
