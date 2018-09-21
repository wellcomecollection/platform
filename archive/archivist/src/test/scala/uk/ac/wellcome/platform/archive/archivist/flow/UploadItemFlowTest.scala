package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, BagItConfig, BagManifestLocation}
import uk.ac.wellcome.platform.archive.common.fixtures.{AkkaS3, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.{BagItem, BagLocation, BagPath}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

class UploadItemFlowTest extends FunSpec with Matchers with S3 with MockitoSugar with ZipBagItFixture with Akka with AkkaS3 with ScalaFutures with ArchiveJobGenerators{

  it("sends a right of archive item job when uploading a file from an archive item job succeeds"){
    withLocalS3Bucket { bucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          withS3AkkaClient(actorSystem, materializer) { alpakkaS3Client =>
            val fileContent = "bah buh bih beh"
            val namespace = "otherbucket"

            val fileName = "key.txt"
            withZipFile(List(FileEntry(s"$namespace/$fileName", fileContent))) { zipFile =>

              implicit val s = actorSystem
              implicit val m = materializer
              val digest = "52dbe81fda7f771f83ed4afc9a7c156d3bf486f8d654970fa5c5dbebb4ff7b73"

              val bagName = BagPath(randomAlphanumeric())

              val archiveItemJob = createArchiveItemJob(zipFile, bucket, digest, bagName, fileName, namespace)

              val source = Source.single(archiveItemJob)
              val flow = UploadItemFlow()(alpakkaS3Client)
              val futureResult = source via flow runWith Sink.head

              whenReady(futureResult) { result =>
                result shouldBe Right(archiveItemJob)
                getContentFromS3(bucket, s"archive/$bagName/$fileName") shouldBe fileContent
              }

            }
          }
        }
      }
    }
  }
}
