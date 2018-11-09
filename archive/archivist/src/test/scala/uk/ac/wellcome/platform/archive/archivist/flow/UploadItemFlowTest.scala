package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  FileNotFoundError,
  UploadError
}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.ExternalIdentifier
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
    with ArchiveJobGenerators
    with Inside {

  it(
    "sends a right of archive item job when uploading a file from an archive item job succeeds") {
    withLocalS3Bucket { bucket =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val fileContent = "bah buh bih beh"
          val fileName = "key.txt"
          withZipFile(List(FileEntry(s"$fileName", fileContent))) { zipFile =>
            val bagIdentifier =
              ExternalIdentifier(randomAlphanumeric())

            val archiveItemJob =
              createArchiveItemJob(zipFile, bucket, bagIdentifier, fileName)

            val source = Source.single(archiveItemJob)
            val flow = UploadItemFlow(10)(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              result shouldBe Right(archiveItemJob)
              getContentFromS3(
                bucket,
                s"archive/${archiveItemJob.archiveJob.bagLocation.bagPath}/$fileName") shouldBe fileContent
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

            val bagIdentifier =
              ExternalIdentifier(randomAlphanumeric())

            val archiveItemJob =
              createArchiveItemJob(zipFile, bucket, bagIdentifier, fileName)

            val source = Source.single(archiveItemJob)
            val flow = UploadItemFlow(10)(s3Client)
            val futureResult = source via flow runWith Sink.seq

            whenReady(futureResult) { result =>
              result shouldBe List(
                Left(FileNotFoundError(fileName, archiveItemJob)))
              val exception = intercept[AmazonS3Exception] {
                getContentFromS3(
                  bucket,
                  s"archive/${archiveItemJob.archiveJob.bagLocation.bagPath}/$bagIdentifier/$fileName")
              }
              exception.getErrorCode shouldBe "NoSuchKey"
            }
          }
        }
      }
    }
  }

  it(
    "sends a left of archive item job when uploading a big file fails because the bucket does not exist (Resume supervision strategy)") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        val fileContent = "bah buh bih beh"
        val fileName = "key.txt"
        withZipFile(List(FileEntry(s"$fileName", fileContent))) { zipFile =>
          val bagIdentifier =
            ExternalIdentifier(randomAlphanumeric())

          val failingArchiveItemJob = createArchiveItemJob(
            zipFile,
            Bucket("does-not-exist"),
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
            inside(result.toList) {
              case List(Left(UploadError(location, exception, t))) =>
                location shouldBe failingArchiveItemJob.uploadLocation
                t shouldBe failingArchiveItemJob
                exception shouldBe a[AmazonS3Exception]
                exception
                  .asInstanceOf[AmazonS3Exception]
                  .getErrorCode shouldBe "NoSuchBucket"
            }
          }
        }
      }
    }
  }
}
