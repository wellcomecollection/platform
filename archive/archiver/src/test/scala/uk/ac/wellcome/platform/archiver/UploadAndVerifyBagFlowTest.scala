package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archiver.flow.UploadAndVerifyBagFlow
import uk.ac.wellcome.platform.archiver.models.{
  BagItConfig,
  BagUploaderConfig,
  UploadConfig
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

import scala.concurrent.ExecutionContext.Implicits.global

class UploadAndVerifyBagFlowTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with fixtures.Archiver {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  //    SupervisedMaterializer.create(
  //    "archiver", mock[MetricsSender])(system)

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
        implicit val s3Client = s3AkkaClient

        val bagUploaderConfig = createBagUploaderConfig(storageBucket)
        val bagName = randomAlphanumeric()
        val (zipFile, _) = createBagItZip(bagName, 1)

        val uploader = UploadAndVerifyBagFlow(bagUploaderConfig)

        val (_, verification) =
          uploader.runWith(Source.single(zipFile), Sink.ignore)

        whenReady(verification) { _ =>
          // Do nothing
        }
      }
    }
  }

  it("fails when verifying and uploading an invalid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        implicit val s3Client = s3AkkaClient

        val bagUploaderConfig = createBagUploaderConfig(storageBucket)
        val bagName = randomAlphanumeric()
        val (zipFile, _) = createBagItZip(bagName, 1, false)

        val uploader = UploadAndVerifyBagFlow(bagUploaderConfig)

        val (_, verification) =
          uploader.runWith(Source.single(zipFile), Sink.ignore)

        whenReady(verification.failed) { e =>
          println(e)
        // Do nothing
        }
      }
    }
  }
}
