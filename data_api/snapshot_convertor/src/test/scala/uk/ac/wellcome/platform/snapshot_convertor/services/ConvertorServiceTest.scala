package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import org.scalatest.{Assertion, FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.{IdentifiedWork, IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.platform.snapshot_convertor.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures.{Akka, S3, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class ConvertorServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with GzipUtils
    with ExtendedPatience {

  private def withConvertorService(bucketName: String, actorSystem: ActorSystem, s3AkkaClient: S3Client)(
    testWith: TestWith[ConvertorService, Assertion]) = {
    val convertorService = new ConvertorService(
      actorSystem = actorSystem,
      s3Client = s3AkkaClient,
      s3Endpoint = localS3EndpointUrl
    )

    testWith(convertorService)
  }

  it("completes a conversion successfully") {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        implicit val materializer = ActorMaterializer()(actorSystem)
        withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
          withConvertorService(bucketName, actorSystem, s3AkkaClient) { convertorService =>

            // Create a collection of works.  These three differ by version,
            // if not anything more interesting!
            val works = (1 to 3).map { version =>
              IdentifiedWork(
                canonicalId = "rbfhv6b4",
                title = Some("Rumblings from a rambunctious rodent"),
                sourceIdentifier = SourceIdentifier(
                  identifierScheme = IdentifierSchemes.miroImageNumber,
                  ontologyType = "work",
                  value = "R0060400"
                ),
                version = version
              )
            }

            val elasticsearchJsons = works.map { work =>
              s"""{"_index": "jett4fvw", "_type": "work", "_id": "${work.canonicalId}", "_score": 1, "_source": ${toJson(work).get}}"""
            }
            val content = elasticsearchJsons.mkString("\n")

            withGzipCompressedS3Key(bucketName, content) { objectKey =>
              val conversionJob = ConversionJob(
                bucketName = bucketName,
                objectKey = objectKey
              )

              val future = convertorService.runConversion(conversionJob)

              whenReady(future) { result =>
                result shouldBe CompletedConversionJob(
                  conversionJob = conversionJob,
                  targetLocation = s"http://localhost:33333/$bucketName/target.txt.gz"
                )
              }
            }
          }
        }
      }
    }
  }
}
