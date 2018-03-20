package uk.ac.wellcome.platform.snapshot_convertor.fixtures

import akka.actor.ActorSystem
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.platform.snapshot_convertor.services.ConvertorService
import uk.ac.wellcome.test.fixtures.{AkkaFixtures, S3, TestWith}

case class ConvertorServiceFixtures(
  convertorService: ConvertorService,
  actorSystem: ActorSystem,
  bucketName: String
)

trait ConvertorServiceFixture extends AkkaFixtures with S3 {
  def withConvertorService[R](
    testWith: TestWith[ConvertorServiceFixtures, R]): R = {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>

        withActorSystem { actorSystem =>
          val convertorService = new ConvertorService(
            bucketName = bucketName,
            actorSystem = actorSystem,
            awsConfig = AWSConfig(region = "localhost")
          )

          testWith(ConvertorServiceFixtures(
            convertorService, actorSystem, bucketName))
        }
      }
    }
  }
}
