package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
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

import scala.concurrent.Future

class DownloadAndVerifyDigestItemFlowTest
    extends FunSpec
    with S3
    with ZipBagItFixture
    with ScalaFutures
    with Akka
    with MockitoSugar
    with ArchiveJobGenerators
    with Inside {

  val flow = DownloadAndVerifyDigestItemFlow(parallelism = 10)(s3Client)

  it("passes through a correct right archive item job") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        withZipFile(List()) { file =>
          val fileContent = "bah buh bih beh"
          val fileName = "key.txt"

          val archiveItemJob = createArchiveDigestItemJobWith(
            file = file,
            bucket = bucket,
            digest = sha256(fileContent),
            s3Key = fileName
          )

          s3Client.putObject(
            bucket.name,
            s"${archiveItemJob.archiveJob.bagLocation.completePath}/$fileName",
            fileContent)

          val source = Source.single(archiveItemJob)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Right(archiveItemJob)
          }
        }
      }
    }
  }

  it("outputs a left of archive item job if the checksum doesn't match") {
    withLocalS3Bucket { bucket =>
      withMaterializer { implicit materializer =>
        withZipFile(List()) { file =>
          val fileContent = "bah buh bih beh"
          val digest = "bad-digest"
          val fileName = "key.txt"

          val archiveItemJob = createArchiveDigestItemJobWith(
            file = file,
            bucket = bucket,
            digest = digest,
            s3Key = fileName
          )

          s3Client.putObject(
            bucket.name,
            s"${archiveItemJob.archiveJob.bagLocation.completePath}/$fileName",
            fileContent)

          val source = Source.single(archiveItemJob)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Left(
              ChecksumNotMatchedOnDownloadError(
                expectedChecksum = digest,
                actualChecksum = sha256(fileContent),
                t = archiveItemJob
              )
            )
          }
        }
      }
    }
  }

  it("returns a left of archive item job if the file does not exist") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withZipFile(List()) { file =>
          val archiveItemJob = createArchiveDigestItemJobWith(
            file = file,
            bucket = bucket
          )
          val source = Source.single(archiveItemJob)
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

  def sha256(s: String)(implicit materializer: ActorMaterializer): String = {
    val future: Future[String] =
      Source
        .single(ByteString(s.getBytes))
        .via(SHA256Flow())
        .runWith(Sink.head)

    whenReady(future) { result =>
      result
    }
  }
}
