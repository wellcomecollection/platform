package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.storage.ObjectLocation

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ZipFileDownloadFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixture
    with IngestRequestContextGenerators with Inside {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  it("downloads a zipfile from s3") {
    withLocalS3Bucket { storageBucket =>
      withBagItZip() {
        case (bagName, zipFile) =>
          val downloadZipFlow = ZipFileDownloadFlow(10)(
            s3Client
          )

          val uploadKey = bagName.toString

          s3Client.putObject(
            storageBucket.name,
            uploadKey,
            new File(zipFile.getName))

          val objectLocation = ObjectLocation(storageBucket.name, uploadKey)
          val ingestBagRequest =
            createIngestBagRequestWith(ingestBagLocation = objectLocation)

          val download
            : Future[Either[ArchiveError[IngestBagRequest], ZipFileDownloadComplete]]  =
            downloadZipFlow
              .runWith(Source.single(ingestBagRequest), Sink.head)
              ._2

          whenReady(download) { result =>
            inside(result) {
              case Right(ZipFileDownloadComplete(downloadedZipFile, _)) =>
                zipFile.entries.asScala.toList
                  .map(_.toString) should contain theSameElementsAs downloadedZipFile.entries.asScala.toList
                  .map(_.toString)
                zipFile.size shouldEqual downloadedZipFile.size
            }
          }
      }
    }
  }
}
