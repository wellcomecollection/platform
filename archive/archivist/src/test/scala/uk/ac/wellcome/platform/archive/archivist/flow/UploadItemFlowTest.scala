package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, Supervision}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.DigitisedStorageType
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.Akka

class UploadItemFlowTest
    extends FunSpec
    with Matchers
    with S3
    with MockitoSugar
    with ZipBagItFixture
    with Akka
    with ScalaFutures
    with ArchiveJobGenerators {

  it(
    "sends a right of archive item job when uploading a file from an archive item job succeeds") {
    withLocalS3Bucket { bucket =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val fileContent = "bah buh bih beh"

          val fileName = "key.txt"
          withZipFile(List(FileEntry(s"$fileName", fileContent))) {
            zipFile =>
              val digest =
                "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"

              val bagIdentifier = randomAlphanumeric()

              val archiveItemJob = createArchiveItemJob(
                zipFile,
                bucket,
                digest,
                bagIdentifier,
                fileName)

              val source = Source.single(archiveItemJob)
              val flow = UploadItemFlow(10)(s3Client)
              val futureResult = source via flow runWith Sink.head

              whenReady(futureResult) { result =>
                result shouldBe Right(archiveItemJob)
                getContentFromS3(bucket, s"archive/$DigitisedStorageType/$bagIdentifier/$fileName") shouldBe fileContent
              }

          }
        }
      }
    }
  }

  it(
    "sends a left of archive item job when uploading a file with wrong digest") {
    withLocalS3Bucket { bucket =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val fileContent = "bah buh bih beh"

          val fileName = "key.txt"
          withZipFile(List(FileEntry(s"$fileName", fileContent))) {
            zipFile =>
              val digest =
                "wrong!"

              val bagIdentifier = randomAlphanumeric()

              val archiveItemJob = createArchiveItemJob(
                zipFile,
                bucket,
                digest,
                bagIdentifier,
                fileName)

              val source = Source.single(archiveItemJob)
              val flow = UploadItemFlow(10)(s3Client)
              val futureResult = source via flow runWith Sink.head

              whenReady(futureResult) { result =>
                result shouldBe Left(archiveItemJob)
                getContentFromS3(bucket, s"archive/$DigitisedStorageType/$bagIdentifier/$fileName") shouldBe fileContent
              }

          }
        }
      }
    }
  }

  it(
    "sends a left of archive item job when uploading a file fails because the file does not exist") {
    withLocalS3Bucket { bucket =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withZipFile(List()) { zipFile =>
            val fileName = "key.txt"
            val digest =
              "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"

            val bagIdentifier = randomAlphanumeric()

            val archiveItemJob = createArchiveItemJob(
              zipFile,
              bucket,
              digest,
              bagIdentifier,
              fileName)

            val source = Source.single(archiveItemJob)
            val flow = UploadItemFlow(10)(s3Client)
            val futureResult = source via flow runWith Sink.seq

            whenReady(futureResult) { result =>
              result shouldBe List(Left(archiveItemJob))
              val exception = intercept[AmazonS3Exception] {
                getContentFromS3(bucket, s"archive/$DigitisedStorageType/$bagIdentifier/$fileName")
              }
              exception.getErrorCode shouldBe "NoSuchKey"
            }
          }
        }
      }
    }
  }

  it(
    "sends a left of archive item job when uploading a file fails because the bucket does not exist (Resume supervision strategy)") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        val fileContent = "bah buh bih beh"

        val fileName = "key.txt"
        withZipFile(List(FileEntry(s"$fileName", fileContent))) {
          zipFile =>
            val digest =
              "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"

            val bagIdentifier = randomAlphanumeric()

            val failingArchiveItemJob = createArchiveItemJob(
              zipFile,
              Bucket("does-not-exist"),
              digest,
              bagIdentifier,
              fileName)

            val source = Source.single(failingArchiveItemJob)
            val decider: Supervision.Decider = { e =>
              error("Stream failure", e)
              Supervision.Resume
            }
            val flow = UploadItemFlow(10)(s3Client)
              .withAttributes(ActorAttributes.supervisionStrategy(decider))
            val futureResult = source via flow runWith Sink.seq

            whenReady(futureResult) { result =>
              result shouldBe List(Left(failingArchiveItemJob))
            }

        }
      }
    }
  }

}
