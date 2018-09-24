package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.common.models.BagPath
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.Akka

class DownloadItemFlowTest extends FunSpec with S3 with ZipBagItFixture with ScalaFutures with Akka with MockitoSugar with ArchiveJobGenerators{

  it("passes through a correct right archive item job") {
    withLocalS3Bucket { bucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          withZipFile(List()) { zipFile =>
            implicit val s = actorSystem
            implicit val m = materializer

            val fileContent = "bah buh bih beh"
            val digest = "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"
            val fileName = "key.txt"

            val bagName = BagPath(randomAlphanumeric())

            s3Client.putObject(bucket.name, s"archive/$bagName/$fileName", fileContent)

            val archiveItemJob = createArchiveItemJob(zipFile, bucket, digest, bagName, fileName, "basepath")

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
  }

  it("transforms a right archive job into a left if the checksum doesn't match") {
    withLocalS3Bucket { bucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) {materializer =>
          withZipFile(List()) { zipFile =>
            implicit val s = actorSystem
            implicit val m = materializer

            val fileContent = "bah buh bih beh"
            val digest = "bad-digest"
            val fileName = "key.txt"

            val bagName = BagPath(randomAlphanumeric())

            s3Client.putObject(bucket.name, s"archive/$bagName/$fileName", fileContent)

            val archiveItemJob = createArchiveItemJob(zipFile, bucket, digest, bagName, fileName, "basepath")

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
  }

  it("passes through a left archive item job") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) {materializer =>
        withZipFile(List()) { zipFile =>
          implicit val s = actorSystem
          implicit val m = materializer

          val bucket = Bucket("bucket")
          val digest = "digest"
          val fileName = "key.txt"

          val bagName = BagPath(randomAlphanumeric())
          val archiveItemJob = createArchiveItemJob(zipFile, bucket, digest, bagName, fileName, "basepath")
          val source = Source.single(Left(archiveItemJob))
          val flow = DownloadItemFlow()(s3Client)
          val futureResult = source via flow runWith Sink.head

          whenReady(futureResult) { result =>
            result shouldBe Left(archiveItemJob)
          }
        }
      }
    }
  }

  it("returns a left of archive item job if the file does not exist") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
          withLocalS3Bucket { bucket =>
            withZipFile(List()) { zipFile =>
              implicit val s = actorSystem
              implicit val m = materializer

              val digest = "digest"
              val fileName = "this/does/not/exist.txt"

              val bagName = BagPath(randomAlphanumeric())
              val archiveItemJob = createArchiveItemJob(zipFile, bucket, digest, bagName, fileName, "basepath")
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
  }

}
