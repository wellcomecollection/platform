package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.platform.snapshot_generator.modules.AkkaS3ClientModule
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.fixtures.TestWith

trait AkkaS3 extends S3 {

  def withS3AkkaClient[R](
    actorSystem: ActorSystem,
    materializer: ActorMaterializer)(testWith: TestWith[S3Client, R]): R = {

    val s3AkkaClient = AkkaS3ClientModule.buildAkkaS3Client(
      awsConfig = AWSConfig(region = regionName),
      actorSystem = actorSystem,
      endpoint = localS3EndpointUrl,
      accessKey = accessKey,
      secretKey = secretKey
    )

    testWith(s3AkkaClient)
  }
}
