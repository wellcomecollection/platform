package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.File
import java.util.zip.ZipFile

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.archivist.models.{
  BagItConfig,
  BagUploaderConfig,
  IngestRequestContextGenerators,
  UploadConfig
}
import uk.ac.wellcome.platform.archive.archivist.Archivist
import uk.ac.wellcome.platform.archive.common.fixtures.FileEntry
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{
  BagInfo,
  ExternalIdentifier,
  IngestBagRequest,
  NotificationMessage
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait ArchivistFixtures
    extends Messaging
    with ZipBagItFixture
    with IngestRequestContextGenerators {

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
    dataFileCount: Int = 12,
    createDigest: String => String = createValidDigest,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: BagInfo => Option[FileEntry] = createValidBagInfoFile)(
    testWith: TestWith[(IngestBagRequest, ExternalIdentifier), R]): Boolean =
    withBagItZip(
      dataFileCount = dataFileCount,
      createDigest = createDigest,
      createDataManifest = createDataManifest,
      createBagItFile = createBagItFile,
      createBagInfoFile = createBagInfoFile
    ) {
      case (bagIdentifier, zipFile) =>
        sendBag(zipFile, ingestBucket, queuePair)(ingestBagRequest =>
          testWith((ingestBagRequest, bagIdentifier)))
    }

  def withApp[R](storageBucket: Bucket,
                 queuePair: QueuePair,
                 registrarTopic: Topic,
                 progressTopic: Topic)(testWith: TestWith[Archivist, R]): R =
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        val archivist = new Archivist(
          s3Client = s3Client,
          snsClient = snsClient,
          messageStream = new MessageStream[NotificationMessage, Unit](
            actorSystem = actorSystem,
            sqsClient = asyncSqsClient,
            sqsConfig = createSQSConfigWith(queuePair.queue),
            metricsSender = metricsSender
          ),
          bagUploaderConfig = BagUploaderConfig(
            uploadConfig = UploadConfig(uploadNamespace = storageBucket.name),
            parallelism = 10,
            bagItConfig = BagItConfig()
          ),
          snsRegistrarConfig = createSNSConfigWith(registrarTopic),
          snsProgressConfig = createSNSConfigWith(progressTopic)
        )(
          actorSystem = actorSystem
        )

        testWith(archivist)
      }
    }

  def withArchivist[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, Topic, Archivist), R])
    : R = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic {
        registrarTopic =>
          withLocalSnsTopic {
            progressTopic =>
              withLocalS3Bucket { ingestBucket =>
                withLocalS3Bucket { storageBucket =>
                  withApp(
                    storageBucket,
                    queuePair,
                    registrarTopic,
                    progressTopic) { archivist =>
                    testWith(
                      (
                        ingestBucket,
                        storageBucket,
                        queuePair,
                        registrarTopic,
                        progressTopic,
                        archivist))
                  }
                }
              }
          }
      }
    })
  }

}
