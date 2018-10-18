package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerator
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveJob,
  IngestRequestContextGenerators
}
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveJobError,
  ChecksumNotMatchedOnUploadError,
  FileNotFoundError
}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.error.InvalidBagManifestError
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagId,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.progress.Progress
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
    with Inside
    with ProgressUpdateAssertions {

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
                  result shouldBe List(Right(ArchiveComplete(
                    ingestContext.archiveRequestId,
                    BagId(
                      ingestContext.storageSpace,
                      bagName
                    ),
                    BagLocation(
                      storageBucket.name,
                      "archive",
                      BagPath(s"${ingestContext.storageSpace}/$bagName"))
                  )))

                  assertTopicReceivesProgressEventUpdate(
                    ingestContext.archiveRequestId,
                    reportingTopic) { events =>
                    inside(events) {
                      case List(event) =>
                        event.description shouldBe "Bag uploaded and verified successfully"
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

                  assertTopicReceivesProgressStatusUpdate(
                    ingestContext.archiveRequestId,
                    reportingTopic,
                    Progress.Failed) { events =>
                    events should have size (zipFile.entries().asScala.size - 1)
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
                  result shouldBe List(
                    Left(FileNotFoundError("bag-info.txt", ingestContext)))

                  assertTopicReceivesProgressStatusUpdate(
                    ingestContext.archiveRequestId,
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
                      archiveJob shouldBe a[ArchiveJob]
                      archiveJob
                        .asInstanceOf[ArchiveJob]
                        .bagLocation shouldBe BagLocation(
                        storageBucket.name,
                        "archive",
                        BagPath(s"${ingestContext.storageSpace}/$bagName"))
                  }

                  assertTopicReceivesProgressStatusUpdate(
                    ingestContext.archiveRequestId,
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
