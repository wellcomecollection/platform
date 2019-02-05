package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, Supervision}
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

  val flow = UploadItemFlow(parallelism = 10)(s3Client)

  it(
    "sends a right of archive item job when uploading a file from an archive item job succeeds") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        val fileContent = "bah buh bih beh"
        val fileName = "key.txt"
        withZipFile(List(FileEntry(fileName, fileContent))) { file =>
          val archiveItemJob = createArchiveItemJobWith(
            file = file,
            bucket = bucket,
            s3Key = fileName
          )

          val source = Source.single(archiveItemJob)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe 'right
            result.right.get._1 shouldBe archiveItemJob

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

          val archiveItemJob = createArchiveItemJobWith(
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
      val fileContent = "bah buh bih beh"
      val fileName = "key.txt"
      withZipFile(List(FileEntry(s"$fileName", fileContent))) { file =>
        val failingArchiveItemJob = createArchiveItemJobWith(
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
