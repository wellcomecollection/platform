package uk.ac.wellcome.platform.archiver

import java.io.File

import com.google.inject.Guice
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archiver.BagItUtils._
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

class ArchiverFeatureTest extends FunSpec
  with Matchers
  with ScalaFutures
  with Messaging
  with AkkaS3 {


  case class Bag(bagName: String)

  def withBag[R](ingestBucket: Bucket, queuePair: QueuePair, valid: Boolean = true)(testWith: TestWith[Bag, R]) = {
    val bagName = randomAlphanumeric()
    val (zipFile, fileName) = createBagItZip(bagName, 1, valid)

    val uploadKey = s"upload/path/$bagName.zip"
    s3Client.putObject(ingestBucket.name, uploadKey, new File(fileName))

    val uploadObjectLocation = ObjectLocation(ingestBucket.name, uploadKey)
    sendNotificationToSQS(queuePair.queue, uploadObjectLocation)

    testWith(Bag(bagName))
  }

  def withApp[R](storageBucket: Bucket, queuePair: QueuePair)(testWith: TestWith[Archiver, R]) = {
    val archiver = new Archiver {
      val injector = Guice.createInjector(
        new TestAppConfigModule(queuePair.queue.url, storageBucket.name),
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule
      )
    }

    testWith(archiver)
  }

  def withArchiver[R](testWith: TestWith[(Bucket, Bucket, QueuePair, Archiver), R]) = {
    withLocalSqsQueueAndDlq(queuePair => {
      withLocalS3Bucket { ingestBucket =>
        withLocalS3Bucket { storageBucket =>
          withApp(ingestBucket, queuePair) { archiver =>
            testWith((ingestBucket, storageBucket, queuePair, archiver))
          }
        }
      }
    })
  }

  it("continues after failure") {
    withArchiver { case (ingestBucket, storageBucket, queuePair, archiver) =>
      withBag(ingestBucket, queuePair, false) { invalidBag =>
        withBag(ingestBucket, queuePair, true) { validBag =>

          val _ = archiver.run()

          eventually {
            assertQueueHasSize(queuePair.queue, 0)
            assertQueueHasSize(queuePair.dlq, 1)
          }
        }
      }
    }
  }

  it("downloads, uploads and verifies a BagIt bag") {
    withArchiver { case (ingestBucket, storageBucket, queuePair, archiver) =>
      withBag(ingestBucket, queuePair, false) { invalidBag =>
        archiver.run()

        eventually {
          assertQueueHasSize(queuePair.queue, 0)
        }
      }
    }
  }
}

