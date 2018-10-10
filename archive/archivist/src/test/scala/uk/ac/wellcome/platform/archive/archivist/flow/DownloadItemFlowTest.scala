package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.ChecksumNotMatchedOnDownloadError
import uk.ac.wellcome.platform.archive.common.models.error.DownloadError
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

class DownloadItemFlowTest
  extends FunSpec
    with S3
    with ZipBagItFixture
    with ScalaFutures
    with Akka
    with MockitoSugar
    with ArchiveJobGenerators
    with Inside {

  it("passes through a correct right archive item job") {
    withLocalS3Bucket { bucket =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withZipFile(List()) { zipFile =>
            val fileContent = "bah buh bih beh"
            val digest =
              "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"
            val fileName = "key.txt"

            val bagIdentifier = randomAlphanumeric()

            val archiveItemJob = createArchiveItemJob(
              zipFile,
              bucket,
              digest,
              bagIdentifier,
              fileName)

            s3Client.putObject(
              bucket.name,
              s"archive/${archiveItemJob.archiveJob.bagLocation.bagPath}/$fileName",
              fileContent)


            val source = Source.single(archiveItemJob)
            val flow = DownloadItemFlow(10)(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              result shouldBe Right(archiveItemJob)
            }

          }
        }
      }
    }
  }

  it("outputs a left of archive item job if the checksum doesn't match") {
    withLocalS3Bucket { bucket =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withZipFile(List()) { zipFile =>
            val fileContent = "bah buh bih beh"
            val digest = "bad-digest"
            val fileName = "key.txt"

            val bagIdentifier = randomAlphanumeric()

            val archiveItemJob = createArchiveItemJob(
              zipFile,
              bucket,
              digest,
              bagIdentifier,
              fileName)

            s3Client.putObject(
              bucket.name,
              s"archive/${archiveItemJob.archiveJob.bagLocation.bagPath}/$fileName",
              fileContent)

            val source = Source.single(archiveItemJob)
            val flow = DownloadItemFlow(10)(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              result shouldBe Left(
                ChecksumNotMatchedOnDownloadError(
                  digest,
                  "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73",
                  archiveItemJob))
            }
          }
        }
      }
    }
  }

  it("returns a left of archive item job if the file does not exist") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withZipFile(List()) { zipFile =>
            val digest = "digest"
            val fileName = "this/does/not/exist.txt"

            val bagIdentifier = randomAlphanumeric()
            val archiveItemJob = createArchiveItemJob(
              zipFile,
              bucket,
              digest,
              bagIdentifier,
              fileName)
            val source = Source.single(archiveItemJob)
            val flow = DownloadItemFlow(10)(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              inside(result) {
                case Left(DownloadError(exception, uploadLocation, job)) =>
                  job shouldBe archiveItemJob
                  uploadLocation shouldBe archiveItemJob.uploadLocation
                  exception shouldBe a[AmazonS3Exception]
              }
            }
          }
        }
      }
    }
  }

}
