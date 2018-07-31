package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archiver.flow.VerifiedBagUploaderFlow
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig


class VerifiedBagUploaderTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with AkkaS3 {

  import BagItUtils._

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  it("succeeds when verifying and uploading a valid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        implicit val _ = s3AkkaClient

        val bagUploaderConfig = BagUploaderConfig(uploadNamespace = storageBucket.name)
        val bagName = randomAlphanumeric()
        val zipFile = createBagItZip(bagName, 1)

        val uploader = VerifiedBagUploaderFlow(bagUploaderConfig, zipFile, bagName)

        val verification = uploader.runWith(Sink.ignore)

        whenReady(verification) { _ =>
          // Do nothing
        }
      }
    }
  }

  it("fails when verifying and uploading an invalid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        implicit val _ = s3AkkaClient

        val bagUploaderConfig = BagUploaderConfig(uploadNamespace = storageBucket.name)
        val bagName = randomAlphanumeric()
        val zipFile = createBagItZip(bagName, 1, false)

        val uploader = VerifiedBagUploaderFlow(bagUploaderConfig, zipFile, bagName)

        val verification = uploader.runWith(Sink.ignore)

        whenReady(verification.failed) { e =>
          println(e)
          // Do nothing
        }
      }
    }
  }
}
