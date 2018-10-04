package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerator
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveJobError,
  ChecksumNotMatchedOnUploadError,
  FileNotFoundError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath,
  DigitisedStorageType
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.test.fixtures.Akka
import scala.collection.JavaConverters._

class ArchiveZipFileFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixture
    with IngestRequestContextGenerators
    with BagUploaderConfigGenerator
    with Akka
    with SNS
    with Inside {

  implicit val s3client = s3Client
  implicit val snsclient = snsClient

  it("notifies success when verifying and uploading a valid bag") {
    withLocalS3Bucket { storageBucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            val bagUploaderConfig = createBagUploaderConfig(storageBucket)
            withBagItZip() {
              case (bagName, zipFile) =>
                val uploader = ArchiveZipFileFlow(
                  bagUploaderConfig,
                  SNSConfig(reportingTopic.arn))
                val ingestContext = createIngestBagRequestWith()
                val (_, verification) =
                  uploader.runWith(
                    Source.single(
                      ZipFileDownloadComplete(zipFile, ingestContext)),
                    Sink.seq
                  )

                whenReady(verification) { result =>
                  listKeysInBucket(storageBucket) should have size 4
                  result shouldBe List(
                    Right(
                      ArchiveComplete(
                        ingestContext.archiveRequestId,
                        BagLocation(
                          storageBucket.name,
                          "archive",
                          BagPath(s"$DigitisedStorageType/$bagName")),
                        None)))

                  val messages = listMessagesReceivedFromSNS(reportingTopic)
                  messages should have size 1
                  val progressUpdate =
                    fromJson[ProgressUpdate](messages.head.message).get
                  inside(progressUpdate) {
                    case ProgressUpdate(id, List(event), status) =>
                      id shouldBe ingestContext.archiveRequestId
                      status shouldBe Progress.None

                      event.description shouldBe "Bag uploaded and verified successfully"
                  }
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
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            val bagUploaderConfig = createBagUploaderConfig(storageBucket)
            withBagItZip(createDigest = _ => "bad_digest") {
              case (_, zipFile) =>
                val uploader = ArchiveZipFileFlow(
                  bagUploaderConfig,
                  SNSConfig(reportingTopic.arn))
                val ingestContext = createIngestBagRequest

                val (_, verification) =
                  uploader.runWith(
                    Source.single(
                      ZipFileDownloadComplete(zipFile, ingestContext)),
                    Sink.seq)

                whenReady(verification) { result =>
                  inside(result.toList) {
                    case List(Left(ArchiveJobError(_, errors))) =>
                      all(errors) shouldBe a[ChecksumNotMatchedOnUploadError]
                  }
                  val messages = listMessagesReceivedFromSNS(reportingTopic)
                  messages should have size 1
                  val progressUpdate =
                    fromJson[ProgressUpdate](messages.head.message).get
                  inside(progressUpdate) {
                    case ProgressUpdate(id, events, status) =>
                      id shouldBe ingestContext.archiveRequestId
                      status shouldBe Progress.Failed

                      events should have size (zipFile
                        .entries()
                        .asScala
                        .size - 1)

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
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            val bagUploaderConfig = createBagUploaderConfig(storageBucket)
            withBagItZip(createBagInfoFile = _ => None) {
              case (_, zipFile) =>
                val uploader = ArchiveZipFileFlow(
                  bagUploaderConfig,
                  SNSConfig(reportingTopic.arn))
                val ingestContext = createIngestBagRequest

                val (_, verification) =
                  uploader.runWith(
                    Source.single(
                      ZipFileDownloadComplete(zipFile, ingestContext)),
                    Sink.seq)

                whenReady(verification) { result =>
                  inside(result.toList) {
                    case List(Left(FileNotFoundError(path, ingestRequest))) =>
                      ingestRequest shouldBe ingestContext
                      path shouldBe "bag-info.txt"
                  }

                  val messages = listMessagesReceivedFromSNS(reportingTopic)
                  messages should have size 1
                  val progressUpdate =
                    fromJson[ProgressUpdate](messages.head.message).get
                  inside(progressUpdate) {
                    case ProgressUpdate(id, List(event), status) =>
                      id shouldBe ingestContext.archiveRequestId
                      status shouldBe Progress.Failed
                      event.description shouldBe result.head.left.get.toString
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
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            val bagUploaderConfig = createBagUploaderConfig(storageBucket)
            withBagItZip(createDataManifest = _ =>
              Some(FileEntry("manifest-sha256.txt", "dgfssjhdfg"))) {
              case (bagName, zipFile) =>
                val uploader = ArchiveZipFileFlow(
                  bagUploaderConfig,
                  SNSConfig(reportingTopic.arn))
                val ingestContext = createIngestBagRequest

                val (_, verification) =
                  uploader.runWith(
                    Source.single(
                      ZipFileDownloadComplete(zipFile, ingestContext)),
                    Sink.seq)

                whenReady(verification) { result =>
                  inside(result.toList) {
                    case List(
                        Left(InvalidBagManifestError(
                          archiveJob,
                          "manifest-sha256.txt"))) =>
                      archiveJob.bagLocation shouldBe BagLocation(
                        storageBucket.name,
                        "archive",
                        BagPath(s"$DigitisedStorageType/$bagName"))
                  }

                  val messages = listMessagesReceivedFromSNS(reportingTopic)
                  messages should have size 1
                  val progressUpdate =
                    fromJson[ProgressUpdate](messages.head.message).get
                  inside(progressUpdate) {
                    case ProgressUpdate(id, List(event), status) =>
                      id shouldBe ingestContext.archiveRequestId
                      status shouldBe Progress.Failed
                      event.description shouldBe result.head.left.get.toString
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
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val bagUploaderConfig = createBagUploaderConfig(storageBucket)
          withBagItZip(createDigest = _ => "bad_digest") {
            case (_, zipFile) =>
              val uploader =
                ArchiveZipFileFlow(bagUploaderConfig, SNSConfig("bad-topic"))
              val ingestContext = createIngestBagRequest

              val (_, verification) =
                uploader.runWith(
                  Source.single(
                    ZipFileDownloadComplete(zipFile, ingestContext)),
                  Sink.seq)

              whenReady(verification) { result =>
                result shouldBe empty
              }
          }
        }
      }
    }

  }
}
