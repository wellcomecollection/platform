package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerator
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveJobError,
  ChecksumNotMatchedOnUploadError,
  FileNotFoundError
}
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveJob,
  IngestRequestContextGenerators
}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

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
            withBagItZip() {
              case (bagName, zipFile) =>
                withArchiveZipFileFlow(storageBucket, reportingTopic) {
                  uploader =>
                    val ingestContext = createIngestBagRequestWith()
                    val (_, verification) =
                      uploader.runWith(
                        Source.single(
                          ZipFileDownloadComplete(zipFile, ingestContext)),
                        Sink.seq
                      )

                    whenReady(verification) { result =>
                      listKeysInBucket(storageBucket) should have size 5
                      result shouldBe List(Right(ArchiveComplete(
                        ingestContext.archiveRequestId,
                        ingestContext.storageSpace,
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
  }

  it(
    "notifies failure when verifying and uploading a bag with incorrect digests") {
    withLocalS3Bucket { storageBucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            withBagItZip(createDigest = _ => "bad_digest") {
              case (_, zipFile) =>
                withArchiveZipFileFlow(storageBucket, reportingTopic) {
                  uploader =>
                    val ingestContext = createIngestBagRequest

                    val (_, verification) =
                      uploader.runWith(
                        Source.single(
                          ZipFileDownloadComplete(zipFile, ingestContext)),
                        Sink.seq)

                    whenReady(verification) { result =>
                      inside(result.toList) {
                        case List(Left(ArchiveJobError(_, errors))) =>
                          all(errors) shouldBe a[
                            ChecksumNotMatchedOnUploadError]
                      }

                      assertTopicReceivesProgressStatusUpdate(
                        ingestContext.archiveRequestId,
                        reportingTopic,
                        Progress.Failed,
                        None) { events =>
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

  }

  it(
    "notifies failure when verifying and uploading a bag with no bag-info.txt file") {
    withLocalS3Bucket { storageBucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            withBagItZip(createBagInfoFile = _ => None) {
              case (_, zipFile) =>
                withArchiveZipFileFlow(storageBucket, reportingTopic) {
                  uploader =>
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
                        Progress.Failed,
                        None) { events =>
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

  }

  it(
    "notifies failure when verifying and uploading a bag with invalid data manifest file") {
    withLocalS3Bucket { storageBucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withLocalSnsTopic { reportingTopic =>
            withBagItZip(createDataManifest = _ =>
              Some(FileEntry("manifest-sha256.txt", "dgfssjhdfg"))) {
              case (bagName, zipFile) =>
                withArchiveZipFileFlow(storageBucket, reportingTopic) {
                  uploader =>
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
                        Progress.Failed,
                        None) { events =>
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

  }

  it("doesn't output if notifying progress fails") {
    withLocalS3Bucket { storageBucket =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withBagItZip(createDigest = _ => "bad_digest") {
            case (_, zipFile) =>
              withArchiveZipFileFlow(storageBucket, Topic("bad-topic")) {
                uploader =>
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

  private def withArchiveZipFileFlow[R](bucket: Bucket, topic: Topic)(
    testWith: TestWith[Flow[ZipFileDownloadComplete,
                            Either[ArchiveError[_], ArchiveComplete],
                            NotUsed],
                       R]): R = {
    val bagUploaderConfig = createBagUploaderConfig(bucket)
    val flow = ArchiveZipFileFlow(
      config = bagUploaderConfig,
      snsConfig = createSNSConfigWith(topic)
    )

    testWith(flow)
  }
}
