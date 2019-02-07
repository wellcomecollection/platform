package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import uk.ac.wellcome.config.core.models.AWSClientConfig
import uk.ac.wellcome.platform.snapshot_generator.config.builders.AkkaS3Builder
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.fixtures.TestWith

trait AkkaS3 extends S3 {

  def withS3AkkaClient[R](endpoint: String)(testWith: TestWith[S3Client, R])(
    implicit actorSystem: ActorSystem,
    materializer: ActorMaterializer): R = {
    val s3AkkaClient = AkkaS3Builder.buildAkkaS3Client(
      awsClientConfig = AWSClientConfig(
        accessKey = Some(accessKey),
        secretKey = Some(secretKey),
        endpoint = Some(endpoint),
        region = "localhost"
      )
    )

    testWith(s3AkkaClient)
  }

  def withS3AkkaClient[R](testWith: TestWith[S3Client, R])(
    implicit actorSystem: ActorSystem,
    materializer: ActorMaterializer): R =
    withS3AkkaClient(endpoint = localS3EndpointUrl) { s3AkkaClient =>
      testWith(s3AkkaClient)
    }
}
