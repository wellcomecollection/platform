package uk.ac.wellcome.platform.snapshot_generator.config.builders

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.impl.ListBucketVersion2
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.auth.{
  AWSCredentialsProvider,
  AWSStaticCredentialsProvider,
  BasicAWSCredentials,
  DefaultAWSCredentialsProviderChain
}
import com.amazonaws.regions.AwsRegionProvider
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.models.AWSClientConfig
import uk.ac.wellcome.typesafe.config.builders.AWSClientConfigBuilder

object AkkaS3Builder extends AWSClientConfigBuilder with Logging {

  def buildAkkaS3Client(config: Config)(
    implicit actorSystem: ActorSystem,
    materializer: ActorMaterializer): S3Client =
    buildAkkaS3Client(
      buildAWSClientConfig(config, namespace = "s3")
    )

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

  def buildAkkaS3Client(awsClientConfig: AWSClientConfig)(
    implicit actorSystem: ActorSystem,
    materializer: ActorMaterializer): S3Client = {
    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = awsClientConfig.region
      }

    val credentialsProvider = if (awsClientConfig.endpoint.isEmpty) {
      DefaultAWSCredentialsProviderChain.getInstance()
    } else {
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(
          awsClientConfig.accessKey.get,
          awsClientConfig.secretKey.get)
      )
    }

    val endpointUrl = awsClientConfig.endpoint match {
      case Some(e) => if (e.isEmpty) None else Some(e)
      case None    => None
    }

    val settings = akkaS3Settings(
      credentialsProvider = credentialsProvider,
      regionProvider = regionProvider,
      endpointUrl = endpointUrl
    )

    debug(s"creating S3 Akka client with settings=[$settings]")
    new S3Client(settings)
  }
}
