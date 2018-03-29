package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.stream.ActorMaterializer
import org.scalatest.{Assertion, FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.test.fixtures.{Akka, S3, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience

class ConvertorServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with ExtendedPatience {

  private def withConvertorService(bucketName: String)(
    testWith: TestWith[ConvertorService, Assertion]) = {
    withActorSystem { actorSystem =>
      implicit val materializer = ActorMaterializer()(actorSystem)
      withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
        val convertorService = new ConvertorService(
          actorSystem = actorSystem,
          s3Client = s3AkkaClient,
          s3Endpoint = localS3EndpointUrl
        )

        testWith(convertorService)
      }
    }
  }

  it("completes a conversion successfully") {
    withLocalS3Bucket { bucketName =>
      withConvertorService { convertorService =>

        // Create a collection of works.  These three differ by version,
        // if not anything more interesting!
        val works = (1 to 3).map { version =>
          IdentifiedWork(
            canonicalId = "t83tggem",
            title = Some("Tired of troubling tests"),
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.miroImageNumber,
              ontologyType = "work",
              value = "T0083000"
            ),
            version = version
          )
        }
      }
    }
  }
}
