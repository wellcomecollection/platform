package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.File
import java.util.zip.ZipFile

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.archivist.Archivist
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerators
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{
  BagInfo,
  IngestBagRequest,
  NotificationMessage
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait ArchivistFixtures
    extends Messaging
    with ZipBagItFixture
    with BagUploaderConfigGenerators
    with IngestBagRequestGenerators {

  import IngestBagRequest._

  def sendBag[R](zipFile: ZipFile, ingestBucket: Bucket, queuePair: QueuePair)(
    testWith: TestWith[IngestBagRequest, R]): R = {

    val ingestBagRequest = createIngestBagRequestWith(
      ingestBagLocation = ObjectLocation(
        ingestBucket.name,
        s"${randomAlphanumeric()}.zip"
      )
    )

    val bucket = ingestBagRequest.zippedBagLocation.namespace
    val key = ingestBagRequest.zippedBagLocation.key

    s3Client.putObject(bucket, key, new File(zipFile.getName))

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
                 registrarTopic: Topic,
                 progressTopic: Topic)(testWith: TestWith[Archivist, R]): R =
    withActorSystem { implicit actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        val archivist = new Archivist(
          s3Client = s3Client,
          snsClient = snsClient,
          messageStream = new MessageStream[NotificationMessage, Unit](
            sqsClient = asyncSqsClient,
            sqsConfig = createSQSConfigWith(queuePair.queue),
            metricsSender = metricsSender
          ),
          bagUploaderConfig = createBagUploaderConfigWith(storageBucket),
          snsRegistrarConfig = createSNSConfigWith(registrarTopic),
          snsProgressConfig = createSNSConfigWith(progressTopic)
        )

        archivist.run()

        testWith(archivist)
      }
    }

  def withArchivist[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, Topic), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(5) { queuePair =>
      withLocalSnsTopic { registrarTopic =>
        withLocalSnsTopic { progressTopic =>
          withLocalS3Bucket { ingestBucket =>
            withLocalS3Bucket { storageBucket =>
              withApp(storageBucket, queuePair, registrarTopic, progressTopic) {
                _ =>
                  testWith(
                    (
                      ingestBucket,
                      storageBucket,
                      queuePair,
                      registrarTopic,
                      progressTopic))
              }
            }
          }
        }
      }
    }
  }
}
