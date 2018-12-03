package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Sink, Source}
import org.apache.commons.io.IOUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors._
import uk.ac.wellcome.platform.archive.archivist.models.BagItConfig
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.error.InvalidBagManifestError
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

import scala.collection.JavaConverters._

class ArchiveJobDigestItemsFlowTest
    extends FunSpec
    with ArchiveJobGenerators
    with S3
    with Akka
    with ScalaFutures
    with ZipBagItFixture
    with Inside
    with IngestBagRequestGenerators {
  implicit val s = s3Client

  it("outputs a right of archive complete if all of the items succeed") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2) { zipFile =>
            val ingestRequest = createIngestBagRequest
            val archiveJob = createArchiveJobWith(
              zipFile = zipFile,
              bucket = bucket
            )
            val source = Source.single(archiveJob)
            val flow = createFlow(ingestRequest)
            val eventualArchiveJobs = source via flow runWith Sink.seq
            whenReady(eventualArchiveJobs) { archiveJobs =>
              archiveJobs shouldBe List(
                Right(
                  ArchiveComplete(
                    ingestRequest.archiveRequestId,
                    ingestRequest.storageSpace,
                    archiveJob.bagLocation
                  )
                )
              )
            }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive error if one of the items fails because it does not exist in the zipFile") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createDataManifest = dataManifestWithNonExistingFile) { zipFile =>
            val ingestRequest = createIngestBagRequest
            val archiveJob = createArchiveJobWith(
              zipFile = zipFile,
              bucket = bucket
            )
            val source = Source.single(archiveJob)
            val flow = createFlow(ingestRequest)
            val eventualArchiveJobs = source via flow runWith Sink.seq
            whenReady(eventualArchiveJobs) { archiveJobs =>
              inside(archiveJobs.toList) {
                case List(
                    Left(
                      ArchiveJobError(
                        actualArchiveJob,
                        List(FileNotFoundError(
                          "this/does/not/exists.jpg",
                          archiveItemJob))))) =>
                  actualArchiveJob shouldBe archiveJob
                  archiveItemJob.bagDigestItem.location shouldBe EntryPath(
                    "this/does/not/exists.jpg")
                  archiveItemJob.archiveJob shouldBe archiveJob
              }

            }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive error if all of the items fail because the checksum is incorrect") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2, createDigest = _ => "bad-digest") {
            zipFile =>
              val ingestRequest = createIngestBagRequest
              val archiveJob = createArchiveJobWith(
                zipFile = zipFile,
                bucket = bucket
              )

              val failedFiles: List[String] = zipFile
                .entries()
                .asScala
                .collect {
                  case entry if !entry.getName.contains("tagmanifest") =>
                    entry.getName
                }
                .toList

              val source = Source.single(archiveJob)
              val flow = createFlow(ingestRequest)
              val eventualArchiveJobs = source via flow runWith Sink.seq
              whenReady(eventualArchiveJobs) { archiveJobs =>
                inside(archiveJobs.toList) {
                  case List(Left(ArchiveJobError(actualArchiveJob, errors))) =>
                    actualArchiveJob shouldBe archiveJob
                    all(errors) shouldBe a[ChecksumNotMatchedOnUploadError]
                    errors.map(_.t.bagDigestItem.location.path) should contain theSameElementsAs failedFiles
                    errors.map(_.t.archiveJob).distinct shouldBe List(
                      archiveJob)
                }
              }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive error if one of the items fail because the checksum is incorrect") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createDataManifest = dataManifestWithWrongChecksum) { zipFile =>
            val ingestRequest = createIngestBagRequest
            val manifestZipEntry = zipFile.getEntry("manifest-sha256.txt")
            val badChecksumLine = IOUtils
              .toString(zipFile.getInputStream(manifestZipEntry), "UTF-8")
              .split("\n")
              .filter(_.contains("badDigest"))
              .head
            val filepath = badChecksumLine.replace("badDigest", "").trim

            val archiveJob = createArchiveJobWith(
              zipFile = zipFile,
              bucket = bucket
            )
            val source = Source.single(archiveJob)
            val flow = createFlow(ingestRequest)
            val eventualArchiveJobs = source via flow runWith Sink.seq
            whenReady(eventualArchiveJobs) { archiveJobs =>
              inside(archiveJobs.toList) {
                case List(Left(ArchiveJobError(job, List(error)))) =>
                  error shouldBe a[ChecksumNotMatchedOnUploadError]
                  val checksumNotMatchedOnUploadError =
                    error.asInstanceOf[ChecksumNotMatchedOnUploadError]
                  checksumNotMatchedOnUploadError.t.archiveJob shouldBe archiveJob
                  checksumNotMatchedOnUploadError.t.bagDigestItem.location shouldBe EntryPath(
                    filepath)
                  job shouldBe archiveJob
              }
            }
          }
        }
      }
    }
  }

  it("outputs a left of archive error if the data manifest is missing") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2, createDataManifest = _ => None) {
            zipFile =>
              val ingestRequest = createIngestBagRequest
              val archiveJob = createArchiveJobWith(
                zipFile = zipFile,
                bucket = bucket
              )
              val source = Source.single(archiveJob)
              val flow = createFlow(ingestRequest)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(
                  Left(FileNotFoundError("manifest-sha256.txt", archiveJob)))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive error if the data manifest is invalid") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createDataManifest = _ =>
              Some(FileEntry("manifest-sha256.txt", randomAlphanumeric()))) {
            zipFile =>
              val ingestRequest = createIngestBagRequest
              val archiveJob = createArchiveJobWith(
                zipFile = zipFile,
                bucket = bucket
              )
              val source = Source.single(archiveJob)
              val flow = createFlow(ingestRequest)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Left(
                  InvalidBagManifestError(archiveJob, "manifest-sha256.txt")))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive error if the tag manifest is missing") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2, createTagManifest = _ => None) {
            zipFile =>
              val ingestRequest = createIngestBagRequest
              val archiveJob = createArchiveJobWith(
                zipFile = zipFile,
                bucket = bucket
              )
              val source = Source.single(archiveJob)
              val flow = createFlow(ingestRequest)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(
                  Left(FileNotFoundError("tagmanifest-sha256.txt", archiveJob)))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive error if the tag manifest is invalid") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createTagManifest = _ =>
              Some(FileEntry("tagmanifest-sha256.txt", randomAlphanumeric()))) {
            zipFile =>
              val ingestRequest = createIngestBagRequest
              val archiveJob = createArchiveJobWith(
                zipFile = zipFile,
                bucket = bucket
              )
              val source = Source.single(archiveJob)
              val flow = createFlow(ingestRequest)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(
                  Left(InvalidBagManifestError(
                    archiveJob,
                    "tagmanifest-sha256.txt")))
              }
          }
        }
      }
    }
  }

  private def createFlow(ingestRequest: IngestBagRequest) =
    ArchiveJobDigestItemsFlow(
      delimiter = BagItConfig().digestDelimiterRegexp,
      parallelism = 10,
      ingestBagRequest = ingestRequest
    )
}
