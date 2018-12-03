package uk.ac.wellcome.platform.snapshot_generator.config.builders

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions.AwsRegionProvider
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AWSClientConfigBuilder
import uk.ac.wellcome.config.core.models.AWSClientConfig
import uk.ac.wellcome.platform.snapshot_generator.finatra.modules.AkkaS3ClientModule.{akkaS3Settings, logger}

object AkkaS3Builder extends AWSClientConfigBuilder with Logging {
  def buildAkkaS3Client(awsClientConfig: AWSClientConfig)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer): S3Client = {
    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = awsClientConfig.region
      }

    val credentialsProvider = awsClientConfig.endpoint match {
      case None | Some("") => DefaultAWSCredentialsProviderChain.getInstance()
      case Some(_) => new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(awsClientConfig.accessKey.get, awsClientConfig.secretKey.get)
      )
    }

    val endpointUrl = awsClientConfig.endpoint match {
      case None | Some("") => None
      case s: Some[String] => s
    }

    val settings = akkaS3Settings(
      credentialsProvider = credentialsProvider,
      regionProvider = regionProvider,
      endpointUrl = endpointUrl
    )

    logger.debug(s"creating S3 Akka client with settings=[$settings]")
    new S3Client(settings)
  }


  def buildAkkaS3Client(config: Config)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer): S3Client =
    buildAkkaS3Client(
      awsClientConfig = buildAWSClientConfig(config, namespace = "s3")
    )
}
