package uk.ac.wellcome.platform.snapshot_convertor.fixtures

import akka.actor.ActorSystem
import com.twitter.finatra.json.FinatraObjectMapper
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.platform.snapshot_convertor.services.ConvertorService
import uk.ac.wellcome.test.fixtures.{S3, AkkaFixtures, TestWith}

case class ConvertorServiceFixtures(
  convertorService: ConvertorService,
  actorSystem: ActorSystem,
  bucketName: String
)

trait ConvertorServiceFixture extends S3 with AkkaFixtures {

  val mapper = FinatraObjectMapper.create()

  def withConvertorService[R](
    testWith: TestWith[ConvertorServiceFixtures, R]): R = {

    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>

            val convertorService = new ConvertorService(
              actorSystem = actorSystem,
              awsConfig = AWSConfig(region = "localhost"),
              s3Client = s3AkkaClient,
              mapper = mapper,
              s3Endpoint = localS3EndpointUrl
            )

            testWith(
              ConvertorServiceFixtures(
                convertorService,
                actorSystem,
                bucketName))

          }
        }
      }
    }
  }
}
