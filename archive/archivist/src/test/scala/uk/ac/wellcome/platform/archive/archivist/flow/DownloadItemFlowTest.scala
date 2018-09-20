package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, BagItConfig, BagManifestLocation}
import uk.ac.wellcome.platform.archive.common.fixtures.{AkkaS3, BagIt}
import uk.ac.wellcome.platform.archive.common.models.{BagItem, BagLocation, BagPath}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.Akka

class DownloadItemFlowTest extends FunSpec with S3 with BagIt with ScalaFutures with Akka with MockitoSugar{

  it("passes through a correct right archive item job") {
    withLocalS3Bucket { bucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) {materializer =>
            implicit val s = actorSystem
            implicit val m = materializer

            val fileContent = "bah buh bih beh"
            val digest = "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"
            val s3Key = "key.txt"

            val bagName = BagPath(randomAlphanumeric())

            s3Client.putObject(bucket.name, s"archive/$bagName/$s3Key", fileContent)

            val archiveItemJob = createArchiveItemJob(bucket, digest, bagName, s3Key)

            val source = Source.single(Right(archiveItemJob))
            val flow = DownloadItemFlow()(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              result shouldBe Right(archiveItemJob)
            }

          }
        }
    }
  }

  it("transforms a right archive job into a left if the checksum doesn't match") {
    withLocalS3Bucket { bucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) {materializer =>
            implicit val s = actorSystem
            implicit val m = materializer

            val fileContent = "bah buh bih beh"
            val digest = "bad-digest"
            val s3Key = "key.txt"

            val bagName = BagPath(randomAlphanumeric())

            s3Client.putObject(bucket.name, s"archive/$bagName/$s3Key", fileContent)

            val archiveItemJob = createArchiveItemJob(bucket, digest, bagName, s3Key)

            val source = Source.single(Right(archiveItemJob))
            val flow = DownloadItemFlow()(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              result shouldBe Left(archiveItemJob)
            }

          }
        }
    }
  }

  it("passes through a left archive item job") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) {materializer =>
          implicit val s = actorSystem
          implicit val m = materializer

          val bucket = Bucket("bucket")
          val digest = "digest"
          val s3Key = "key.txt"

          val bagName = BagPath(randomAlphanumeric())
          val archiveItemJob = createArchiveItemJob(bucket, digest, bagName, s3Key)
          val source = Source.single(Left(archiveItemJob))
          val flow = DownloadItemFlow()(s3Client)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Left(archiveItemJob)
          }
      }
    }
  }

  it("returns a left of archive item job if the file does not exist") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
          withLocalS3Bucket { bucket =>
            implicit val s = actorSystem
            implicit val m = materializer

            val digest = "digest"
            val s3Key = "this/does/not/exist.txt"

            val bagName = BagPath(randomAlphanumeric())
            val archiveItemJob = createArchiveItemJob(bucket, digest, bagName, s3Key)
            val source = Source.single(Right(archiveItemJob))
            val flow = DownloadItemFlow()(s3Client)
            val futureResult = source via flow runWith Sink.head

            whenReady(futureResult) { result =>
              result shouldBe Left(archiveItemJob)
            }
          }

      }
    }
  }

  private def createArchiveItemJob(bucket: S3.Bucket, digest: String, bagName: BagPath, s3Key: String) = {
    val bagLocation = BagLocation(bucket.name, "archive", bagName)
    val archiveJob = ArchiveJob(mock[ZipFile], bagLocation, BagItConfig(), BagManifestLocation(bagName, "manifest-sha256.txt"))
    val bagDigestItem = BagItem(digest, ObjectLocation("otherbucket", s3Key))
    val archiveItemJob = ArchiveItemJob(archiveJob = archiveJob, bagDigestItem = bagDigestItem)
    archiveItemJob
  }
}
