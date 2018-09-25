package uk.ac.wellcome.platform.archive.archivist.flow

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.{
  Archivist => ArchivistFixture
}
import uk.ac.wellcome.platform.archive.archivist.models.{
  BagItConfig,
  BagUploaderConfig,
  IngestRequestContextGenerators,
  UploadConfig
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

class ArchiveZipFileFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixture
    with IngestRequestContextGenerators {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val s3client = s3Client

  def createBagUploaderConfig(bucket: Bucket) =
    BagUploaderConfig(
      uploadConfig = UploadConfig(
        uploadNamespace = bucket.name
      ),
      bagItConfig = BagItConfig()
    )

  it("succeeds when verifying and uploading a valid bag") {
    withLocalS3Bucket { storageBucket =>
      val bagUploaderConfig = createBagUploaderConfig(storageBucket)
      withBagItZip(dataFileCount = 1) {
        case (bagName, zipFile) =>
          val uploader = ArchiveZipFileFlow(bagUploaderConfig)
          val ingestContext = createIngestBagRequestWith()
          val (_, verification) =
            uploader.runWith(
              Source.single(ZipFileDownloadComplete(zipFile, ingestContext)),
              Sink.seq
            )

          whenReady(verification) { result =>
            listKeysInBucket(storageBucket) should have size 4
            result should have size 1
          }
      }
    }
  }

  it("fails when verifying and uploading an invalid bag") {
    withLocalS3Bucket { storageBucket =>
      val bagUploaderConfig = createBagUploaderConfig(storageBucket)
      withBagItZip(dataFileCount = 1, createDigest = _ => "bad_digest") {
        case (bagName, zipFile) =>
          val uploader = ArchiveZipFileFlow(bagUploaderConfig)
          val ingestContext = createIngestBagRequest

          val (_, verification) =
            uploader.runWith(
              Source.single(ZipFileDownloadComplete(zipFile, ingestContext)),
              Sink.seq)

          whenReady(verification) { result => result shouldBe empty
          }
      }
    }

  }
}
