package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.platform.archive.archivist.fixtures.ArchivistFixtures
import uk.ac.wellcome.platform.archive.archivist.models.FileDownloadComplete
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases.BagDownload
import uk.ac.wellcome.platform.archive.common.config.models.Parallelism
import uk.ac.wellcome.platform.archive.common.errors.FileDownloadingError
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.fixtures.TestWith

import scala.collection.JavaConverters._
import scala.collection.immutable

class ZipFileDownloadFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixtures
    with IngestBagRequestGenerators
    with Inside
    with SNS
    with ProgressUpdateAssertions {

  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val s3client = s3Client
  implicit val snsclient = snsClient

  it("downloads a zipfile from s3") {
    withLocalS3Bucket { storageBucket =>
      withLocalSnsTopic { progressTopic =>
        withZipFileDownloadFlow(progressTopic) {
          downloadZipFlow: ZipFileDownloadFlow =>
            val bagInfo = randomBagInfo
            withBagItZip(bagInfo) { file =>
              val uploadKey = bagInfo.externalIdentifier.toString

              s3Client.putObject(
                storageBucket.name,
                uploadKey,
                file
              )

              val objectLocation = ObjectLocation(storageBucket.name, uploadKey)
              val ingestBagRequest =
                createIngestBagRequestWith(ingestBagLocation = objectLocation)

              val download = downloadZipFlow
                .runWith(Source.single(ingestBagRequest), Sink.head)
                ._2

              whenReady(download) { result =>
                inside(result) {
                  case Right(FileDownloadComplete(downloadedFile, _)) => {
                    val zipFile = new ZipFile(downloadedFile)
                    zipFile.entries.asScala.toList
                      .map(_.toString) should contain theSameElementsAs zipFile.entries.asScala.toList
                      .map(_.toString)
                    zipFile.size shouldEqual zipFile.size
                  }
                }
              }
            }
        }
      }
    }
  }

  it("outputs a left of archive error if the zipfile does not exist in s3") {
    withLocalS3Bucket { storageBucket =>
      withLocalSnsTopic { progressTopic =>
        withZipFileDownloadFlow(progressTopic) { downloadZipFlow =>
          val bagIdentifier = randomAlphanumeric()
          val objectLocation =
            ObjectLocation(storageBucket.name, bagIdentifier.toString)
          val ingestBagRequest =
            createIngestBagRequestWith(ingestBagLocation = objectLocation)

          val download =
            downloadZipFlow
              .runWith(Source.single(ingestBagRequest), Sink.seq)
              ._2

          whenReady(download) { result: immutable.Seq[BagDownload] =>
            result should have size (1)

            inside(result.toList) {
              case List(Left(FileDownloadingError(actualBagRequest, _))) =>
                actualBagRequest shouldBe ingestBagRequest
            }

            assertTopicReceivesProgressStatusUpdate(
              ingestBagRequest.id,
              progressTopic,
              Progress.Failed
            ) { events =>
              events should have size 1
              events.head.description should startWith(
                s"Failed downloading file ${objectLocation.namespace}/${objectLocation.key}")
            }
          }
        }
      }
    }
  }

  type ZipFileDownloadFlow =
    Flow[IngestBagRequest, BagDownload, NotUsed]

  private def withZipFileDownloadFlow[R](topic: Topic)(
    testWith: TestWith[ZipFileDownloadFlow, R]): R = {
    implicit val parallelism = Parallelism(10)

    implicit val tf =
      TransferManagerBuilder.standard().withS3Client(s3Client).build()

    val downloadZipFlow: Flow[IngestBagRequest, BagDownload, NotUsed] =
      ZipFileDownloadFlow(
        snsConfig = createSNSConfigWith(topic)
      )

    testWith(downloadZipFlow)
  }
}
