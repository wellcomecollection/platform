package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.{File, FileOutputStream}
import java.net.URI
import java.util.UUID
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import com.google.inject.Guice
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.archivist.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.archivist.{Archivist => ArchivistApp}
import uk.ac.wellcome.platform.archive.common.fixtures.{AkkaS3, BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.{BagPath, IngestBagRequest}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Archivist
    extends AkkaS3
    with LocalDynamoDb
    with ProgressMonitorFixture
    with Messaging
    with BagIt {

  def sendBag[R](bagName: BagPath,
                 file: File,
                 ingestBucket: Bucket,
                 callbackUri: Option[URI],
                 queuePair: QueuePair)(
    testWith: TestWith[(UUID, ObjectLocation, BagPath), R]) = {
    val uploadKey = s"upload/path/$bagName.zip"

    s3Client.putObject(ingestBucket.name, uploadKey, file)

    val uploadedBagLocation = ObjectLocation(ingestBucket.name, uploadKey)
    val ingestRequestId = UUID.randomUUID()
    sendNotificationToSQS(
      queuePair.queue,
      IngestBagRequest(
        ingestRequestId,
        uploadedBagLocation,
        callbackUri))

    testWith((ingestRequestId, uploadedBagLocation, bagName))
  }

  def createAndSendValidBag[R](ingestBucket: Bucket,
                               callbackUri: Option[URI],
                               queuePair: QueuePair,
                               dataFileCount: Int =12)(
                                testWith: TestWith[(UUID, ObjectLocation, BagPath), R]) = withValidBag(12) {
    case (bagName, _, file) =>
      createValidBagItZip(bagName, dataFileCount)

      sendBag(bagName, file, ingestBucket, callbackUri, queuePair) {
        case (requestId, uploadObjectLocation, bag) =>
          testWith((requestId, uploadObjectLocation, bag))
      }
  }

  def createAndSendInvalidBag[R](ingestBucket: Bucket,
                               callbackUri: Option[URI],
                               queuePair: QueuePair,
                                 dataFileCount: Int =12)(
                                testWith: TestWith[(UUID, ObjectLocation, BagPath), R]) = withInvalidBag(12) {
    case (bagName, _, file) =>
      createInvalidBagItZip(bagName, dataFileCount)

      sendBag(bagName, file, ingestBucket, callbackUri, queuePair) {
        case (requestId, uploadObjectLocation, bag) =>
          testWith((requestId, uploadObjectLocation, bag))
      }
  }

  def withValidBag[R](dataFileCount: Int = 1)(
    testWith: TestWith[(BagPath, ZipFile, File), R]) = {
    val bagName = BagPath(randomAlphanumeric())

    info(s"Creating bag $bagName")

    val (zipFile, file) = createValidBagItZip(bagName, dataFileCount)

    testWith((bagName, zipFile, file))

    file.delete()
  }

  def withInvalidBag[R](dataFileCount: Int = 1)(
    testWith: TestWith[(BagPath, ZipFile, File), R]) = {
    val bagName = BagPath(randomAlphanumeric())

    info(s"Creating bag $bagName")

    val (zipFile, file) = createInvalidBagItZip(bagName, dataFileCount)

    testWith((bagName, zipFile, file))

    file.delete()
  }

  def withApp[R](storageBucket: Bucket,
                 queuePair: QueuePair,
                 topicArn: Topic,
                 progressTable: Table)(testWith: TestWith[ArchivistApp, R]) = {
    val archivist = new ArchivistApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          storageBucket.name,
          topicArn.arn,
          progressTable),
        ConfigModule,
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule,
        ProgressMonitorModule
      )
    }
    testWith(archivist)
  }

  def withArchivist[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, Table, ArchivistApp),
                       R]) = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic {
        snsTopic =>
          withLocalS3Bucket {
            ingestBucket =>
              withLocalS3Bucket {
                storageBucket =>
                  withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) {
                    progressTable =>
                      withApp(storageBucket, queuePair, snsTopic, progressTable) {
                        archivist =>
                          testWith(
                            (
                              ingestBucket,
                              storageBucket,
                              queuePair,
                              snsTopic,
                              progressTable,
                              archivist))
                      }
                  }
              }
          }
      }
    })
  }

  def createZip(files: List[FileEntry]) = {
    val file = File.createTempFile("archivist-test", ".zip")
    val zipFileOutputStream = new FileOutputStream(file)
    val zipOutputStream = new ZipOutputStream(zipFileOutputStream)
    files.foreach {
      case FileEntry(name, contents) =>
        val zipEntry = new ZipEntry(name)
        zipOutputStream.putNextEntry(zipEntry)
        zipOutputStream.write(contents.getBytes)
        zipOutputStream.closeEntry()
    }
    zipOutputStream.close()
    val zipFile = new ZipFile(file)

    (zipFile, file)
  }


  def createValidBagItZip(bagName: BagPath,
                     dataFileCount: Int = 1) = {

    val allFiles = createValidBag(bagName, dataFileCount)

    createZip(allFiles.toList)
  }

  def createInvalidBagItZip(bagName: BagPath,
                     dataFileCount: Int = 1) = {

    val allFiles = createInvalidBag(bagName, dataFileCount)

    createZip(allFiles.toList)
  }
}
