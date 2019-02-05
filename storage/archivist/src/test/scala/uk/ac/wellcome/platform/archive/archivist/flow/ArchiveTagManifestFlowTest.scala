package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.{ZipEntry, ZipFile}

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveItemJobError,
  UploadError
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.Akka

import scala.io.Source.fromInputStream

class ArchiveTagManifestFlowTest
    extends FunSpec
    with S3
    with ScalaFutures
    with Akka
    with ZipBagItFixture
    with ArchiveJobGenerators
    with Inside {

  val flow = ArchiveTagManifestFlow(parallelism = 10)(s3Client)

  it("archives the tag manifest") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        withBagItZip(dataFileCount = 2) { file =>
          val archiveJob = createArchiveJobWith(
            file = file,
            bucket = bucket
          )

          val source = Source.single(archiveJob)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Right(archiveJob)

            val expectedTagManifestStream =
              fromInputStream(
                new ZipFile(file).getInputStream(
                  new ZipEntry("tagmanifest-sha256.txt"))).mkString

            getContentFromS3(
              bucket,
              s"${archiveJob.bagLocation.completePath}/tagmanifest-sha256.txt") shouldBe expectedTagManifestStream
          }
        }
      }
    }
  }

  it("fails uploading the tag manifest") {
    withMaterializer { implicit materializer =>
      withBagItZip(dataFileCount = 2) { file =>
        val bagIdentifier = createExternalIdentifier

        val archiveJob = createArchiveJobWith(
          file = file,
          bagIdentifier = bagIdentifier,
          bucket = Bucket("not-a-valid-bucket")
        )

        val source = Source.single(archiveJob)
        val futureResult = source via flow runWith Sink.head

        whenReady(futureResult) { result =>
          inside(result) {
            case Left(ArchiveItemJobError(job, errors)) =>
              job shouldBe archiveJob
              errors should have size 1
              inside(errors.head) {
                case UploadError(location, exception, t) =>
                  exception shouldBe a[AmazonS3Exception]
                  exception
                    .asInstanceOf[AmazonS3Exception]
                    .getErrorCode shouldBe "NoSuchBucket"
                  location shouldBe ObjectLocation(
                    "not-a-valid-bucket",
                    s"${archiveJob.bagLocation.completePath}/tagmanifest-sha256.txt")
              }
          }
        }
      }
    }
  }
}
