package uk.ac.wellcome.platform.archive.archivist.flow
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.ArchiveJobGenerators
import uk.ac.wellcome.platform.archive.archivist.models.BagItConfig
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka

class ArchiveJobFlowTest
    extends FunSpec
    with ArchiveJobGenerators
    with S3
    with Akka
    with ScalaFutures
    with ZipBagItFixture {
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
                archiveJobs shouldBe List(Left(archiveJob))
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
          withBagItZip(dataFileCount = 2, createDataManifest =  _ => None) {
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
              Some(
                FileEntry("manifest-sha256.txt",
                  randomAlphanumeric()))) {
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
              Some(
                FileEntry(
                  "tagmanifest-sha256.txt",
                  randomAlphanumeric()))) {
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
