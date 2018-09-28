package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.File
import java.net.URI
import java.util.UUID
import java.util.zip.ZipFile

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.archivist.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.archivist.{Archivist => ArchivistApp}
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Archivist
    extends Messaging
    with ZipBagItFixture {

  def sendBag[R](zipFile: ZipFile,
                 ingestBucket: Bucket,
                 callbackUri: Option[URI],
                 queuePair: QueuePair)(
    testWith: TestWith[(UUID, ObjectLocation), R]) = {
    val fileName = s"${randomAlphanumeric()}.zip"
      val uploadKey = s"upload/path/$fileName"

    s3Client.putObject(ingestBucket.name, uploadKey, new File(zipFile.getName))

    val uploadedBagLocation = ObjectLocation(ingestBucket.name, uploadKey)
    val ingestRequestId = UUID.randomUUID()
    sendNotificationToSQS(
      queuePair.queue,
      IngestBagRequest(ingestRequestId, uploadedBagLocation, callbackUri))

    testWith((ingestRequestId, uploadedBagLocation))
  }

  def createAndSendBag[R](
    ingestBucket: Bucket,
    callbackUri: Option[URI],
    queuePair: QueuePair,
    dataFileCount: Int = 12,
    createDigest: String => String = createValidDigest,
    createDataManifest: List[(String, String)] => Option[FileEntry] = createValidDataManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: String => Option[FileEntry] = createValidBagInfoFile)(
    testWith: TestWith[(UUID, ObjectLocation, String), R]) =
    withBagItZip(
      dataFileCount = dataFileCount,
      createDigest = createDigest,
      createDataManifest = createDataManifest,
      createBagItFile = createBagItFile,
      createBagInfoFile = createBagInfoFile) {
      case (bagIdentifier, zipFile) =>
        sendBag(zipFile, ingestBucket, callbackUri, queuePair) {
          case (requestId, uploadObjectLocation) =>
            testWith((requestId, uploadObjectLocation, bagIdentifier))
        }
    }

  def withApp[R](storageBucket: Bucket,
                 queuePair: QueuePair,
                 topicArn: Topic)(testWith: TestWith[ArchivistApp, R]) = {
    val archivist = new ArchivistApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          storageBucket.name,
          topicArn.arn),
        ConfigModule,
        AkkaModule,
        S3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule
      )
    }
    testWith(archivist)
  }

  def withArchivist[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, ArchivistApp),
                       R]) = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic {
        snsTopic =>
          withLocalS3Bucket {
            ingestBucket =>
              withLocalS3Bucket {
                storageBucket =>
                  withApp(storageBucket, queuePair, snsTopic) {
                        archivist =>
                          testWith(
                            (
                              ingestBucket,
                              storageBucket,
                              queuePair,
                              snsTopic,
                              archivist))
                      }

              }
          }
      }
    })
  }

}
