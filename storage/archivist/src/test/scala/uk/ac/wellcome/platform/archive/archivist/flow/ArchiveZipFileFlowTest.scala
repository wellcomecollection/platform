package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.platform.archive.archivist.fixtures.ArchivistFixtures
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerators
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveJob,
  FileDownloadComplete
}
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases.{
  ArchiveCompletion,
  BagDownload
}
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveJobError,
  ChecksumNotMatchedOnUploadError,
  FileNotFoundError
}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models.error.InvalidBagManifestError
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagLocation,
  BagPath
}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.collection.JavaConverters._

class ArchiveZipFileFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixtures
    with IngestBagRequestGenerators
    with BagUploaderConfigGenerators
    with Akka
    with SNS
    with Inside
    with ProgressUpdateAssertions {

  implicit val s3client = s3Client
  implicit val snsclient = snsClient

  it("notifies success when verifying and uploading a valid bag") {
    withLocalS3Bucket { storageBucket =>
      withMaterializer { implicit materializer =>
        withLocalSnsTopic { reportingTopic =>
          val bagInfo = randomBagInfo
          withBagItZip(bagInfo) { zipFile =>
            withArchiveZipFileFlow(storageBucket, reportingTopic) { uploader =>
              val ingestContext = createIngestBagRequest
              val (_, verification) =
                uploader.runWith(
                  Source.single(
                    Right(
                      FileDownloadComplete(zipFile, ingestContext)
                    )
                  ),
                  Sink.seq
                )

              whenReady(verification) { result =>
                listKeysInBucket(storageBucket) should have size 5
                result shouldBe List(Right(ReplicationRequest(
                  archiveRequestId = ingestContext.id,
                  srcBagLocation = BagLocation(
                    storageNamespace = storageBucket.name,
                    storagePrefix = "archive",
                    storageSpace = ingestContext.storageSpace,
                    bagPath = BagPath(bagInfo.externalIdentifier.toString)
                  )
                )))

                assertTopicReceivesProgressEventUpdate(
                  ingestContext.id,
                  reportingTopic) { events =>
                  inside(events) {
                    case List(event) =>
                      event.description shouldBe "Bag uploaded and verified successfully"
                  }
                }

                new File(zipFile.getName).exists() shouldBe false
              }
            }
          }
        }
      }
    }
  }

  it(
    "notifies failure when verifying and uploading a bag with incorrect digests") {
    withLocalS3Bucket { storageBucket =>
      withMaterializer { implicit materializer =>
        withLocalSnsTopic { reportingTopic =>
          withBagItZip(createDigest = _ => "bad_digest") { file =>
            withArchiveZipFileFlow(storageBucket, reportingTopic) { uploader =>
              val ingestContext = createIngestBagRequest

              val zipFileEntriesSize = new ZipFile(file)
                .entries()
                .asScala
                .size

              val (_, verification) =
                uploader.runWith(
                  Source.single(
                    Right(FileDownloadComplete(file, ingestContext))),
                  Sink.seq)

              whenReady(verification) { result =>
                inside(result.toList) {
                  case List(Left(ArchiveJobError(_, errors))) =>
                    all(errors) shouldBe a[ChecksumNotMatchedOnUploadError]
                }

                assertTopicReceivesProgressStatusUpdate(
                  ingestContext.id,
                  reportingTopic,
                  Progress.Failed) { events =>
                  events should have size (zipFileEntriesSize - 1)

                  all(events.map(_.description)) should include regex "Calculated checksum .+ was different from bad_digest"
                }
              }
            }
          }
        }
      }
    }
  }

  it(
    "notifies failure when verifying and uploading a bag with no bag-info.txt file") {
    withLocalS3Bucket { storageBucket =>
      withMaterializer { implicit materializer =>
        withLocalSnsTopic { reportingTopic =>
          withBagItZip(createBagInfoFile = _ => None) { zipFile =>
            withArchiveZipFileFlow(storageBucket, reportingTopic) { uploader =>
              val ingestContext = createIngestBagRequest

              val (_, verification) =
                uploader.runWith(
                  Source.single(
                    Right(FileDownloadComplete(zipFile, ingestContext))),
                  Sink.seq)

              whenReady(verification) { result =>
                result shouldBe List(
                  Left(FileNotFoundError("bag-info.txt", ingestContext)))

                assertTopicReceivesProgressStatusUpdate(
                  ingestContext.id,
                  reportingTopic,
                  Progress.Failed) { events =>
                  inside(events) {
                    case List(event) =>
                      event.description shouldBe result.head.left.get.toString
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  it(
    "notifies failure when verifying and uploading a bag with invalid data manifest file") {
    withLocalS3Bucket { storageBucket =>
      withMaterializer { implicit materializer =>
        withLocalSnsTopic { reportingTopic =>
          val bagInfo = randomBagInfo
          withBagItZip(
            bagInfo,
            createDataManifest =
              _ => Some(FileEntry("manifest-sha256.txt", "dgfssjhdfg"))) {
            zipFile =>
              withArchiveZipFileFlow(storageBucket, reportingTopic) {
                uploader =>
                  val ingestContext = createIngestBagRequest

                  val (_, verification) =
                    uploader.runWith(
                      Source.single(
                        Right(FileDownloadComplete(zipFile, ingestContext))),
                      Sink.seq)

                  whenReady(verification) { result =>
                    inside(result.toList) {
                      case List(
                          Left(
                            InvalidBagManifestError(
                              archiveJob,
                              "manifest-sha256.txt",
                              _))) =>
                        archiveJob shouldBe a[ArchiveJob]
                        archiveJob
                          .asInstanceOf[ArchiveJob]
                          .bagLocation shouldBe BagLocation(
                          storageNamespace = storageBucket.name,
                          storagePrefix = "archive",
                          storageSpace = ingestContext.storageSpace,
                          bagPath = BagPath(bagInfo.externalIdentifier.toString)
                        )
                    }

                    assertTopicReceivesProgressStatusUpdate(
                      ingestContext.id,
                      reportingTopic,
                      Progress.Failed) { events =>
                      inside(events) {
                        case List(event) =>
                          event.description shouldBe result.head.left.get.toString
                      }
                    }
                  }
              }
          }
        }
      }
    }
  }

  it("doesn't output if notifying progress fails") {
    withLocalS3Bucket { storageBucket =>
      withMaterializer { implicit materializer =>
        withBagItZip(createDigest = _ => "bad_digest") { zipFile =>
          withArchiveZipFileFlow(storageBucket, Topic("bad-topic")) {
            uploader =>
              val ingestContext = createIngestBagRequest

              val (_, verification) =
                uploader.runWith(
                  Source.single(
                    Right(FileDownloadComplete(zipFile, ingestContext))),
                  Sink.seq)

              whenReady(verification) { result =>
                result shouldBe empty
              }
          }
        }
      }
    }
  }

  private def withArchiveZipFileFlow[R](bucket: Bucket, topic: Topic)(
    testWith: TestWith[Flow[BagDownload, ArchiveCompletion, NotUsed], R]): R = {
    val bagUploaderConfig = createBagUploaderConfigWith(bucket)
    val flow = ArchiveZipFileFlow(
      config = bagUploaderConfig,
      snsConfig = createSNSConfigWith(topic)
    )

    testWith(flow)
  }
}
