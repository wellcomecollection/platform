package uk.ac.wellcome.platform.archive.archivist.flow
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSpec, Inside}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.BagItConfig
import uk.ac.wellcome.platform.archive.archivist.models.errors.{ArchiveJobError, ChecksumNotMatchedOnUploadError, FileNotFoundError}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.EntryPath
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

import scala.collection.JavaConverters._

class ArchiveJobFlowTest
    extends FunSpec
    with ArchiveJobGenerators
    with S3
    with Akka
    with ScalaFutures
    with ZipBagItFixture with Inside {
  implicit val s = s3Client

  it("outputs a right of archive job if all of the items succeed") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)
              val eventualArchiveJobs = source via flow runWith Sink.seq
              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Right(archiveJob))
              }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive job if one of the items fails because it does not exist in the zipFile") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createDataManifest = dataManifestWithNonExistingFile) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)
              val eventualArchiveJobs = source via flow runWith Sink.seq
              whenReady(eventualArchiveJobs) { archiveJobs =>
              inside(archiveJobs.toList){ case List(Left(ArchiveJobError(actualArchiveJob, List(FileNotFoundError(archiveItemJob))))) =>
                actualArchiveJob shouldBe archiveJob
                archiveItemJob.bagDigestItem.location shouldBe EntryPath("this/does/not/exists.jpg")
              }

              }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive job if all of the items fail because the checksum is incorrect") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2, createDigest = _ => "bad-digest") {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)

              val failedFiles: List[String] = zipFile.entries().asScala.collect{case entry if !entry.getName.contains("tagmanifest") => entry.getName}.toList

              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)
              val eventualArchiveJobs = source via flow runWith Sink.seq
              whenReady(eventualArchiveJobs) { archiveJobs =>
                inside(archiveJobs.toList){ case List(Left(ArchiveJobError(actualArchiveJob, errors))) =>
                  actualArchiveJob shouldBe archiveJob
                  all (errors) shouldBe a[ChecksumNotMatchedOnUploadError]
                  errors.map(_.job.bagDigestItem.location.path) should contain theSameElementsAs failedFiles
                }
              }
          }
        }
      }
    }
  }

  it(
    "outputs a left of archive job if one of the items fail because the checksum is incorrect") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createDataManifest = dataManifestWithWrongChecksum) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)
              val eventualArchiveJobs = source via flow runWith Sink.seq
              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Left(archiveJob))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive job if the data manifest is missing") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2, createDataManifest = _ => None) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Left(archiveJob))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive job if the data manifest is invalid") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createDataManifest = _ =>
              Some(FileEntry("manifest-sha256.txt", randomAlphanumeric()))) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Left(archiveJob))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive job if the tag manifest is missing") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(dataFileCount = 2, createTagManifest = _ => None) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Left(archiveJob))
              }
          }
        }
      }
    }
  }

  it("outputs a left of archive job if the tag manifest is invalid") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalS3Bucket { bucket =>
          withBagItZip(
            dataFileCount = 2,
            createTagManifest = _ =>
              Some(FileEntry("tagmanifest-sha256.txt", randomAlphanumeric()))) {
            case (bagName, zipFile) =>
              val archiveJob = createArchiveJob(zipFile, bagName, bucket)
              val source = Source.single(archiveJob)
              val flow = ArchiveJobFlow(BagItConfig().digestDelimiterRegexp, 10)

              val eventualArchiveJobs = source via flow runWith Sink.seq

              whenReady(eventualArchiveJobs) { archiveJobs =>
                archiveJobs shouldBe List(Left(archiveJob))
              }
          }
        }
      }
    }
  }

}
