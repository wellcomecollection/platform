package uk.ac.wellcome.platform.archive.archivist

import java.util.zip.ZipFile

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.streams.flow.ZipFileDownloadFlow
import uk.ac.wellcome.platform.archive.common.models.IngestRequestContext
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ArchiveProgressMonitorFixture
import uk.ac.wellcome.storage.ObjectLocation

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ZipFileDownloadFlowTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixture
    with ProgressMonitorFixture
    with IngestRequestContextGenerators {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  it("downloads a zipfile from s3") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        withMockProgressMonitor() { archiveProgressMonitor =>
          withBag() {
            case (bagName, zipFile, file) =>
              val downloadZipFlow = ZipFileDownloadFlow()(
                s3AkkaClient
              )

              val uploadKey = bagName.toString

              s3Client.putObject(storageBucket.name, uploadKey, file)

              val objectLocation = ObjectLocation(storageBucket.name, uploadKey)
              val requestContext = createIngestRequestContextWith(
                ingestBagLocation = objectLocation)

              val download: Future[(ZipFile, IngestRequestContext)] =
                downloadZipFlow
                  .runWith(
                    Source.single((objectLocation, requestContext)),
                    Sink.head)
                  ._2

              whenReady(download) {
                case (downloadedZipFile, _) =>
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
}
