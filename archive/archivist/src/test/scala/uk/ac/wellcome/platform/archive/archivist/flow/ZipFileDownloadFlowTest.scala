package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.ZipFileDownloadingError
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.storage.ObjectLocation

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ZipFileDownloadFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixture
    with IngestRequestContextGenerators
    with Inside
    with SNS
    with ProgressUpdateAssertions {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val s3client = s3Client
  implicit val snsclient = snsClient

  it("downloads a zipfile from s3") {
    withLocalS3Bucket { storageBucket =>
      withLocalSnsTopic { progressTopic =>
        withBagItZip() {
          case (bagName, zipFile) =>
            val downloadZipFlow =
              ZipFileDownloadFlow(10, SNSConfig(progressTopic.arn))

            val uploadKey = bagName.toString

            s3Client.putObject(
              storageBucket.name,
              uploadKey,
              new File(zipFile.getName))

            val objectLocation = ObjectLocation(storageBucket.name, uploadKey)
            val ingestBagRequest =
              createIngestBagRequestWith(ingestBagLocation = objectLocation)

            val download: Future[Either[ArchiveError[IngestBagRequest],
                                        ZipFileDownloadComplete]] =
              downloadZipFlow
                .runWith(Source.single(ingestBagRequest), Sink.head)
                ._2

            whenReady(download) { result =>
              inside(result) {
                case Right(ZipFileDownloadComplete(downloadedZipFile, _)) =>
                  downloadedZipFile.entries.asScala.toList
                    .map(_.toString) should contain theSameElementsAs zipFile.entries.asScala.toList
                    .map(_.toString)
                  downloadedZipFile.size shouldEqual zipFile.size
              }
            }
        }
      }
    }
  }

  it("outputs a left of archive error if the zipfile does not exist in s3") {
    withLocalS3Bucket { storageBucket =>
      withLocalSnsTopic { progressTopic =>
        val bagIdentifier = randomAlphanumeric()

        val downloadZipFlow =
          ZipFileDownloadFlow(10, SNSConfig(progressTopic.arn))

        val objectLocation =
          ObjectLocation(storageBucket.name, bagIdentifier.toString)
        val ingestBagRequest =
          createIngestBagRequestWith(ingestBagLocation = objectLocation)

        val download =
          downloadZipFlow
            .runWith(Source.single(ingestBagRequest), Sink.seq)
            ._2

        whenReady(download) { result =>
          inside(result.toList) {
            case List(Left(ZipFileDownloadingError(actualBagRequest, _))) =>
              actualBagRequest shouldBe ingestBagRequest
          }

          assertTopicReceivesProgressStatusUpdate(
            ingestBagRequest.archiveRequestId,
            progressTopic,
            Progress.Failed,
            None) { events =>
            events should have size 1
            events.head.description should startWith(
              s"Failed downloading zipFile ${objectLocation.namespace}/${objectLocation.key}")
          }
        }
      }
    }
  }
}
