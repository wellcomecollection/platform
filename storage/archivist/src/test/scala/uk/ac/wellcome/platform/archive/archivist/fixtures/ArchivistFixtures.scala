package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.File

import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archive.archivist.Archivist
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerators
import uk.ac.wellcome.platform.archive.common.fixtures.{
  ArchiveMessaging,
  FileEntry
}
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.models.bagit.BagInfo
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

trait ArchivistFixtures
    extends Messaging
    with Akka
    with ZipBagItFixture
    with ArchiveMessaging
    with BagUploaderConfigGenerators
    with IngestBagRequestGenerators {

  def sendBag[R](file: File, ingestBucket: Bucket, queuePair: QueuePair)(
    testWith: TestWith[IngestBagRequest, R]): R = {

    val ingestBagRequest = createIngestBagRequestWith(
      ingestBagLocation = ObjectLocation(
        ingestBucket.name,
        s"${randomAlphanumeric()}.zip"
      )
    )

    val bucket = ingestBagRequest.zippedBagLocation.namespace
    val key = ingestBagRequest.zippedBagLocation.key

    s3Client.putObject(bucket, key, file)

    sendNotificationToSQS(
      queuePair.queue,
      ingestBagRequest
    )

    testWith(ingestBagRequest)
  }

  def createAndSendBag[R](
    ingestBucket: Bucket,
    queuePair: QueuePair,
    bagInfo: BagInfo = randomBagInfo,
    dataFileCount: Int = 12,
    createDigest: String => String = createValidDigest,
    createTagManifest: List[(String, String)] => Option[FileEntry] =
      createValidTagManifest,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: BagInfo => Option[FileEntry] = createValidBagInfoFile)(
    testWith: TestWith[IngestBagRequest, R]): R =
    withBagItZip(
      bagInfo = bagInfo,
      dataFileCount = dataFileCount,
      createTagManifest = createTagManifest,
      createDigest = createDigest,
      createDataManifest = createDataManifest,
      createBagItFile = createBagItFile,
      createBagInfoFile = createBagInfoFile
    ) { zipFile =>
      sendBag(zipFile, ingestBucket, queuePair) { ingestBagRequest =>
        testWith(ingestBagRequest)
      }
    }

  def withApp[R](storageBucket: Bucket,
                 queuePair: QueuePair,
                 nextTopic: Topic,
                 progressTopic: Topic,
                 parallelism: Int = 10)(testWith: TestWith[Archivist, R]): R =
    withActorSystem { implicit actorSystem =>
      withArchiveMessageStream[NotificationMessage, Unit, R](queuePair.queue) {
        messageStream =>
          implicit val s3 = s3Client
          implicit val sns = snsClient
          implicit val tf =
            TransferManagerBuilder.standard().withS3Client(s3Client).build()

          val archivist = new Archivist(
            messageStream = messageStream,
            bagUploaderConfig =
              createBagUploaderConfigWith(storageBucket, parallelism),
            snsNextConfig = createSNSConfigWith(nextTopic),
            snsProgressConfig = createSNSConfigWith(progressTopic)
          )

          archivist.run()

          testWith(archivist)
      }
    }

  def withArchivist[R](parallelism: Int = 10)(
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, Topic), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(5) { queuePair =>
      withLocalSnsTopic { nextTopic =>
        withLocalSnsTopic { progressTopic =>
          withLocalS3Bucket { ingestBucket =>
            withLocalS3Bucket { storageBucket =>
              withApp(
                storageBucket,
                queuePair,
                nextTopic,
                progressTopic,
                parallelism) { _ =>
                testWith(
                  (
                    ingestBucket,
                    storageBucket,
                    queuePair,
                    nextTopic,
                    progressTopic))
              }
            }
          }
        }
      }
    }
  }
}
