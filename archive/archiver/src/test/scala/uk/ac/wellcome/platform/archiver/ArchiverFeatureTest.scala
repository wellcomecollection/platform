package uk.ac.wellcome.platform.archiver

import java.io.File

import com.google.inject.Guice
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.platform.archiver.BagItUtils.{createBagItZip, randomAlphanumeric}
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.utils.JsonUtil._

class ArchiverFeatureTest extends FunSpec
  with Matchers
  with ScalaFutures
  with Messaging
  with AkkaS3 {

  it("downloads, uploads and verifies a BagIt bag") {
    withLocalSqsQueueAndDlq(queuePair => {
      withLocalS3Bucket { ingestBucket =>
        withLocalS3Bucket { storageBucket =>
          val bagName = randomAlphanumeric()
          val (zipFile, fileName) = createBagItZip(bagName, 1)

          val uploadKey = "upload/path/file.zip"
          s3Client.putObject(ingestBucket.name, uploadKey, new File(fileName))

          val uploadObjectLocation = ObjectLocation(ingestBucket.name, uploadKey)

          sendNotificationToSQS(queuePair.queue, uploadObjectLocation)

          val app = new Archiver {
            val injector = Guice.createInjector(
              new TestAppConfigModule(queuePair.queue.url, storageBucket.name),
              AkkaModule,
              AkkaS3ClientModule,
              CloudWatchClientModule,
              SQSClientModule
            )
          }

          app.run()
        }
      }
    })
  }
}

