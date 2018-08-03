package uk.ac.wellcome.platform.archiver

import java.io.File
import java.util.zip.ZipFile

import com.google.inject.Guice
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
//import uk.ac.wellcome.platform.archiver.BagItUtils._
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.json.JsonUtil._

class ArchiverFeatureTest extends FunSpec
  with Matchers
  with ScalaFutures
  with Messaging
  with AkkaS3 {

  // TODO: Need to test failure cases!!!
  it("downloads, uploads and verifies a BagIt bag") {
    withLocalSqsQueueAndDlq(queuePair => {
      withLocalS3Bucket { ingestBucket =>
        withLocalS3Bucket { storageBucket =>
          //val bagName = randomAlphanumeric()
          //val (zipFile, fileName) = createBagItZip(bagName, 1)

          //val bagName = "b22454408"
          val fileName = "/Users/k/Desktop/b22454408.zip"
          val zipFile = new ZipFile(fileName)

          val entries = zipFile.entries()
          val fileCount = Stream
            .continually(entries.nextElement)
            .takeWhile(_ => entries.hasMoreElements)
            .toList
            .length

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

          eventually {
            val objects = s3Client.listObjects(storageBucket.name)
            val objectSummaries = objects.getObjectSummaries

            objectSummaries.toArray.length shouldEqual fileCount
          }
        }
      }
    })
  }
}

