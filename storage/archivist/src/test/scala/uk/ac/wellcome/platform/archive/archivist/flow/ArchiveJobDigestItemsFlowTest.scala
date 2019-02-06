package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.scaladsl.{Sink, Source}
import org.apache.commons.io.IOUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors._
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.bagit.BagItemPath
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
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(dataFileCount = 2) { file =>
          val ingestRequest = createIngestBagRequest
          val archiveJob = createArchiveJobWith(file, bucket = bucket)
          val source = Source.single(archiveJob)
          val flow = createFlow(ingestRequest)
          val eventualArchiveJobs = source via flow runWith Sink.seq

          whenReady(eventualArchiveJobs) { archiveJobs =>
            archiveJobs shouldBe List(
              Right(
                ReplicationRequest(
                  archiveRequestId = ingestRequest.id,
                  srcBagLocation = archiveJob.bagLocation
                )
              )
            )
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive error if one of the items fails because it does not exist in the zipFile") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(
          dataFileCount = 2,
          createDataManifest = dataManifestWithNonExistingFile) { file =>
          val ingestRequest = createIngestBagRequest
          val archiveJob = createArchiveJobWith(
            file = file,
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
                archiveItemJob.bagDigestItem.path shouldBe BagItemPath(
                  "this/does/not/exists.jpg")
                archiveItemJob.archiveJob shouldBe archiveJob
            }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive error if all of the items fail because the checksum is incorrect") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(dataFileCount = 2, createDigest = _ => "bad-digest") {
          file =>
            val ingestRequest = createIngestBagRequest
            val archiveJob = createArchiveJobWith(
              file = file,
              bucket = bucket
            )

            val failedFiles: List[String] = new ZipFile(file)
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
                  errors.map { _.t.bagDigestItem.path.toString } should contain theSameElementsAs failedFiles
                  errors.map(_.t.archiveJob).distinct shouldBe List(archiveJob)
              }
            }
        }
      }
    }
  }

  it(
    "outputs a left of archive error if one of the items fail because the checksum is incorrect") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(
          dataFileCount = 2,
          createDataManifest = dataManifestWithWrongChecksum) { file =>
          val zipFile = new ZipFile(file)
          val ingestRequest = createIngestBagRequest
          val manifestZipEntry = zipFile.getEntry("manifest-sha256.txt")
          val badChecksumLine = IOUtils
            .toString(zipFile.getInputStream(manifestZipEntry), "UTF-8")
            .split("\n")
            .filter(_.contains("badDigest"))
            .head
          val filepath = badChecksumLine.replace("badDigest", "").trim

          val archiveJob = createArchiveJobWith(
            file = file,
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
                checksumNotMatchedOnUploadError.t.bagDigestItem.path shouldBe BagItemPath(
                  filepath)
                job shouldBe archiveJob
            }
          }
        }
      }
    }
  }

  it("outputs a left of archive error if the data manifest is missing") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(dataFileCount = 2, createDataManifest = _ => None) {
          file =>
            val ingestRequest = createIngestBagRequest
            val archiveJob = createArchiveJobWith(
              file = file,
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

  it("outputs a left of archive error if the data manifest is invalid") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(
          dataFileCount = 2,
          createDataManifest =
            _ => Some(FileEntry("manifest-sha256.txt", randomAlphanumeric()))) {
          file =>
            val ingestRequest = createIngestBagRequest
            val archiveJob = createArchiveJobWith(
              file = file,
              bucket = bucket
            )
            val source = Source.single(archiveJob)
            val flow = createFlow(ingestRequest)

            val eventualArchiveJobs = source via flow runWith Sink.seq

            whenReady(eventualArchiveJobs) { archiveJobs =>
              inside(archiveJobs) {
                case Vector(
                    Left(
                      InvalidBagManifestError(
                        actualArchiveJob,
                        "manifest-sha256.txt",
                        _))) =>
                  actualArchiveJob shouldBe archiveJob
              }
            }
        }
      }
    }
  }

  it("outputs a left of archive error if the tag manifest is missing") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(dataFileCount = 2, createTagManifest = _ => None) { file =>
          val ingestRequest = createIngestBagRequest
          val archiveJob = createArchiveJobWith(
            file = file,
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

  it("outputs a left of archive error if the tag manifest is invalid") {
    withMaterializer { implicit materializer =>
      withLocalS3Bucket { bucket =>
        withBagItZip(
          dataFileCount = 2,
          createTagManifest = _ =>
            Some(FileEntry("tagmanifest-sha256.txt", randomAlphanumeric()))) {
          file =>
            val ingestRequest = createIngestBagRequest
            val archiveJob = createArchiveJobWith(
              file = file,
              bucket = bucket
            )
            val source = Source.single(archiveJob)
            val flow = createFlow(ingestRequest)

            val eventualArchiveJobs = source via flow runWith Sink.seq

            whenReady(eventualArchiveJobs) { archiveJobs =>
              inside(archiveJobs) {
                case Vector(
                    Left(
                      InvalidBagManifestError(
                        actualArchiveJob,
                        "tagmanifest-sha256.txt",
                        _))) =>
                  actualArchiveJob shouldBe archiveJob
              }
            }
        }
      }
    }
  }

  private def createFlow(ingestRequest: IngestBagRequest) =
    ArchiveJobDigestItemsFlow(
      parallelism = 10,
      ingestBagRequest = ingestRequest
    )
}
