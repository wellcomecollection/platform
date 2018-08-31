package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.flow.{BadChecksum, FailedArchivingException, UploadAndVerifyBagFlow}
import uk.ac.wellcome.platform.archive.archivist.models.{BagItConfig, BagUploaderConfig, IngestRequestContextGenerators, UploadConfig}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.common.models.BagName

import scala.concurrent.ExecutionContext.Implicits.global

class UploadAndVerifyBagFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ArchivistFixture
    with IngestRequestContextGenerators {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  def createBagUploaderConfig(bucket: Bucket) =
    BagUploaderConfig(
      uploadConfig = UploadConfig(
        uploadNamespace = bucket.name
      ),
      bagItConfig = BagItConfig()
    )

  it("succeeds when verifying and uploading a valid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        withMockArchiveProgressMonitor() { archiveProgressMonitor =>
          implicit val s3Client = s3AkkaClient
          implicit val progress = archiveProgressMonitor

          val bagUploaderConfig = createBagUploaderConfig(storageBucket)
          val bagName = BagName(randomAlphanumeric())
          val (zipFile, _) = createBagItZip(bagName, 1)

          val uploader = UploadAndVerifyBagFlow(bagUploaderConfig)
          val ingestContext = createIngestRequestContextWith()
          val (_, verification) =
            uploader.runWith(Source.single((zipFile, ingestContext)), Sink.ignore)

          whenReady(verification) { _ =>
            listKeysInBucket(storageBucket) should have size 5
          }
        }
      }
    }
  }

  it("fails when verifying and uploading an invalid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        withMockArchiveProgressMonitor() { archiveProgressMonitor =>
          implicit val s3Client = s3AkkaClient
          implicit val progress = archiveProgressMonitor

          val bagUploaderConfig = createBagUploaderConfig(storageBucket)
          val bagName = BagName(randomAlphanumeric())
          val (zipFile, _) = createBagItZip(bagName, 1, false)

          val uploader = UploadAndVerifyBagFlow(bagUploaderConfig)
          val ingestContext = createIngestRequestContextWith()

          val (_, verification) =
            uploader.runWith(Source.single((zipFile, ingestContext)), Sink.ignore)

          whenReady(verification.failed) { actualException =>
            val expectedException =
              FailedArchivingException(bagName, Seq(BadChecksum()))
            actualException shouldBe expectedException
          }
        }
      }
    }
  }
}
