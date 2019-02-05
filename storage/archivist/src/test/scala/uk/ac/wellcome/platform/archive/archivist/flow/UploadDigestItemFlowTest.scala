package uk.ac.wellcome.platform.archive.archivist.flow

import java.nio.charset.StandardCharsets

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, Supervision}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Entry, FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ChecksumNotMatchedOnUploadError,
  FileNotFoundError,
  UploadDigestItemError
}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.Akka

class UploadDigestItemFlowTest
    extends FunSpec
    with Matchers
    with S3
    with MockitoSugar
    with ZipBagItFixture
    with Akka
    with ScalaFutures
    with ArchiveJobGenerators
    with Inside {

  val flow = UploadDigestItemFlow(parallelism = 10)(s3Client)

  it(
    "sends a right of archive item job when uploading a file from an archive item job succeeds") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        val fileContent = "bah buh bih beh"

        val fileName = "key.txt"
        withZipFile(List(FileEntry(s"$fileName", fileContent))) { file =>
          val digest =
            "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"

          val archiveItemJob = createArchiveDigestItemJobWith(
            file = file,
            bucket = bucket,
            digest = digest,
            s3Key = fileName
          )

          val source = Source.single(archiveItemJob)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Right(archiveItemJob)
            val s3Key =
              s"${archiveItemJob.archiveJob.bagLocation.completePath}/$fileName"

            val storedObject = s3Client.getObject(bucket.name, s3Key)
            storedObject.getObjectMetadata.getUserMetadata should contain only Entry(
              "sha-256",
              digest)

            getContentFromS3(bucket, s3Key) shouldBe fileContent
          }

        }
      }
    }
  }

  it("sends a left of archive item job when uploading a file with wrong digest") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        val fileContent = "bah buh bih beh"

        val fileName = "key.txt"
        withZipFile(List(FileEntry(s"$fileName", fileContent))) { file =>
          val digest = "wrong!"

          val archiveItemJob = createArchiveDigestItemJobWith(
            file = file,
            bucket = bucket,
            digest = digest,
            s3Key = fileName
          )

          val source = Source.single(archiveItemJob)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Left(ChecksumNotMatchedOnUploadError(
              digest,
              "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73",
              archiveItemJob))
            getContentFromS3(
              bucket,
              s"${archiveItemJob.archiveJob.bagLocation.completePath}/$fileName") shouldBe fileContent
          }

        }
      }
    }
  }

  it(
    "sends a left of archive item job when uploading a file fails because the file does not exist") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        withZipFile(List()) { file =>
          val fileName = "key.txt"
          val bagIdentifier = createExternalIdentifier

          val archiveItemJob = createArchiveDigestItemJobWith(
            file = file,
            bucket = bucket,
            bagIdentifier = bagIdentifier,
            s3Key = fileName
          )

          val source = Source.single(archiveItemJob)
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

  it(
    "sends a left of archive item job when uploading a big file fails because the bucket does not exist (Resume supervision strategy)") {
    withMaterializer { implicit materializer =>
      val bytes = Array.fill(23 * 1024 * 1024)(
        (scala.util.Random.nextInt(256) - 128).toByte)
      val fileContent = new String(bytes, StandardCharsets.UTF_8)

      val fileName = "key.txt"
      withZipFile(List(FileEntry(s"$fileName", fileContent))) { file =>
        val failingArchiveItemJob = createArchiveDigestItemJobWith(
          file = file,
          bucket = Bucket("does-not-exist"),
          s3Key = fileName
        )

        val source = Source.single(failingArchiveItemJob)
        val decider: Supervision.Decider = { e =>
          error("Stream failure", e)
          Supervision.Resume
        }
        val modifiedFlow = flow
          .withAttributes(ActorAttributes.supervisionStrategy(decider))
        val futureResult = source via modifiedFlow runWith Sink.seq

        whenReady(futureResult) { result =>
          inside(result.toList) {
            case List(Left(UploadDigestItemError(exception, job))) =>
              job shouldBe failingArchiveItemJob
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
