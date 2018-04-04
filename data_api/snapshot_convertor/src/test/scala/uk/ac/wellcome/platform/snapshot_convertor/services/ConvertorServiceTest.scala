package uk.ac.wellcome.platform.snapshot_convertor.services

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest}
import org.scalatest.{Assertion, FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.display.models.{AllWorksIncludes, DisplayWork}
import uk.ac.wellcome.exceptions.GracefulFailureException
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

  private def withConvertorService(bucketName: String,
                                   actorSystem: ActorSystem,
                                   s3AkkaClient: S3Client)(
    testWith: TestWith[ConvertorService, Assertion]) = {
    val convertorService = new ConvertorService(
      actorSystem = actorSystem,
      s3Client = s3Client,
      akkaS3Client = s3AkkaClient,
      s3Endpoint = localS3EndpointUrl
    )

    testWith(convertorService)
  }

  it("completes a conversion successfully") {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        implicit val materializer = ActorMaterializer()(actorSystem)
        withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
          withConvertorService(bucketName, actorSystem, s3AkkaClient) {
            convertorService =>
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
                s"""{"_index": "jett4fvw", "_type": "work", "_id": "${work.canonicalId}", "_score": 1, "_source": ${toJson(
                  work).get}}"""
              }
              val content = elasticsearchJsons.mkString("\n")

              withGzipCompressedS3Key(bucketName, content) { objectKey =>
                val conversionJob = ConversionJob(
                  bucketName = bucketName,
                  objectKey = objectKey
                )

                val future = convertorService.runConversion(conversionJob)

                whenReady(future) { result =>
                  val downloadFile =
                    File.createTempFile("convertorServiceTest", ".txt.gz")
                  s3Client.getObject(
                    new GetObjectRequest(bucketName, "target.txt.gz"),
                    downloadFile)

                  val contents = readGzipFile(downloadFile.getPath)
                  val expectedContents = works
                    .map { DisplayWork(_, includes = AllWorksIncludes()) }
                    .map { toJson(_).get }
                    .mkString("\n") + "\n"

                  contents shouldBe expectedContents

                  result shouldBe CompletedConversionJob(
                    conversionJob = conversionJob,
                    targetLocation =
                      s"http://localhost:33333/$bucketName/target.txt.gz"
                  )
                }
              }
          }
        }
      }
    }
  }

  // This test is meant to catch an error we saw when we first turned on
  // the snapshot convertor:
  //
  //    akka.http.scaladsl.model.EntityStreamSizeException:
  //    EntityStreamSizeException: actual entity size (Some(19403836)) exceeded
  //    content length limit (8388608 bytes)! You can configure this by setting
  //    `akka.http.[server|client].parsing.max-content-length` or calling
  //    `HttpEntity.withSizeLimit` before materializing the dataBytes stream.
  //
  // With the original code, we were unable to read anything more than
  // an 8MB file from S3.  This test deliberately creates a very large file,
  // and tries to stream it back out.
  //
  it("completes a very large conversion successfully") {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        implicit val materializer = ActorMaterializer()(actorSystem)
        withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
          withConvertorService(bucketName, actorSystem, s3AkkaClient) {
            convertorService =>
              // Create a collection of works.  These three differ by version,
              // if not anything more interesting!
              val works = (1 to 100000).map { version =>
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
                s"""{"_index": "jett4fvw", "_type": "work", "_id": "${work.canonicalId}", "_score": 1, "_source": ${toJson(
                  work).get}}"""
              }
              val content = elasticsearchJsons.mkString("\n")

              withGzipCompressedS3Key(bucketName, content) { objectKey =>
                val conversionJob = ConversionJob(
                  bucketName = bucketName,
                  objectKey = objectKey
                )

                val future = convertorService.runConversion(conversionJob)

                whenReady(future) { result =>
                  val downloadFile =
                    File.createTempFile("convertorServiceTest", ".txt.gz")
                  s3Client.getObject(
                    new GetObjectRequest(bucketName, "target.txt.gz"),
                    downloadFile)

                  val contents = readGzipFile(downloadFile.getPath)
                  val expectedContents = works
                    .map { DisplayWork(_, includes = AllWorksIncludes()) }
                    .map { toJson(_).get }
                    .mkString("\n") + "\n"

                  contents shouldBe expectedContents

                  result shouldBe CompletedConversionJob(
                    conversionJob = conversionJob,
                    targetLocation =
                      s"http://localhost:33333/$bucketName/target.txt.gz"
                  )
                }
              }
          }
        }
      }
    }
  }

  it("returns a failed future if asked to convert a non-existent snapshot") {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        implicit val materializer = ActorMaterializer()(actorSystem)
        withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
          withConvertorService(bucketName, actorSystem, s3AkkaClient) {
            convertorService =>
              val conversionJob = ConversionJob(
                bucketName = bucketName,
                objectKey = "doesnotexist.txt.gz"
              )

              val future = convertorService.runConversion(conversionJob)

              whenReady(future.failed) { result =>
                result shouldBe a[AmazonS3Exception]
              }
          }
        }
      }
    }
  }

  it("returns a failed future if asked to convert a malformed snapshot") {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        implicit val materializer = ActorMaterializer()(actorSystem)
        withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
          withConvertorService(bucketName, actorSystem, s3AkkaClient) {
            convertorService =>
              withGzipCompressedS3Key(
                bucketName,
                content = "This is not what snapshots look like") {
                objectKey =>
                  val conversionJob = ConversionJob(
                    bucketName = bucketName,
                    objectKey = objectKey
                  )

                  val future = convertorService.runConversion(conversionJob)

                  whenReady(future.failed) { result =>
                    result shouldBe a[GracefulFailureException]
                  }
              }
          }
        }
      }
    }
  }

  it("returns a failed future if the S3 upload fails") {
    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        implicit val materializer = ActorMaterializer()(actorSystem)
        withS3AkkaClient(actorSystem, materializer) { s3AkkaClient =>
          withConvertorService(bucketName, actorSystem, s3AkkaClient) {
            convertorService =>
              // Create a collection of works.  These three differ by version,
              // if not anything more interesting!
              val works = (1 to 3).map { version =>
                IdentifiedWork(
                  canonicalId = "h4dh3esm",
                  title = Some("Harrowing Henry is hardly heard from"),
                  sourceIdentifier = SourceIdentifier(
                    identifierScheme = IdentifierSchemes.miroImageNumber,
                    ontologyType = "work",
                    value = "r4f2t3bf"
                  ),
                  version = version
                )
              }

              val elasticsearchJsons = works.map { work =>
                s"""{"_index": "jett4fvw", "_type": "work", "_id": "${work.canonicalId}", "_score": 1, "_source": ${toJson(
                  work).get}}"""
              }
              val content = elasticsearchJsons.mkString("\n")

              withGzipCompressedS3Key(bucketName, content) { objectKey =>
                val conversionJob = ConversionJob(
                  bucketName = "wrongBukkit",
                  objectKey = objectKey
                )

                val future = convertorService.runConversion(conversionJob)

                whenReady(future.failed) { result =>
                  result shouldBe a[AmazonS3Exception]
                }
              }
          }
        }
      }
    }
  }
}
