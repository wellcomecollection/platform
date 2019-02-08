package uk.ac.wellcome.platform.archive.bagreplicator.fixtures

import java.util.UUID

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.scalatest.Assertion
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archive.bagreplicator.BagReplicator
import uk.ac.wellcome.platform.archive.bagreplicator.config.{
  BagReplicatorConfig,
  ReplicatorDestinationConfig
}
import uk.ac.wellcome.platform.archive.common.fixtures.{
  ArchiveMessaging,
  BagLocationFixtures,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagInfo,
  BagLocation
}
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.collection.JavaConverters._

trait BagReplicatorFixtures
    extends S3
    with RandomThings
    with Messaging
    with Akka
    with BagLocationFixtures
    with ArchiveMessaging {

  def withBagNotification[R](
    queuePair: QueuePair,
    storageBucket: Bucket,
    archiveRequestId: UUID = randomUUID,
    storageSpace: StorageSpace = randomStorageSpace,
    bagInfo: BagInfo = randomBagInfo
  )(testWith: TestWith[BagLocation, R]): R =
    withBag(storageBucket, bagInfo = bagInfo, storageSpace = storageSpace) {
      bagLocation =>
        val replicationRequest = ReplicationRequest(
          archiveRequestId = archiveRequestId,
          srcBagLocation = bagLocation
        )

        sendNotificationToSQS(
          queuePair.queue,
          replicationRequest
        )

        testWith(bagLocation)
    }

  def withBagReplicator[R](
    queuePair: QueuePair,
    progressTopic: Topic,
    outgoingTopic: Topic,
    dstBucket: Bucket,
    dstRootPath: String)(testWith: TestWith[BagReplicator, R]): R =
    withActorSystem { implicit actorSystem =>
      withArchiveMessageStream[NotificationMessage, Unit, R](queuePair.queue) {
        messageStream =>
          val bagReplicator = new BagReplicator(
            s3Client = s3Client,
            snsClient = snsClient,
            messageStream = messageStream,
            bagReplicatorConfig = BagReplicatorConfig(
              parallelism = 10,
              ReplicatorDestinationConfig(dstBucket.name, dstRootPath)),
            progressSnsConfig = createSNSConfigWith(progressTopic),
            outgoingSnsConfig = createSNSConfigWith(outgoingTopic)
          )

          bagReplicator.run()

          testWith(bagReplicator)
      }
    }

  def withApp[R](
    testWith: TestWith[(Bucket, QueuePair, Bucket, String, Topic, Topic), R])
    : R = {
    withLocalSqsQueueAndDlqAndTimeout(15) { queuePair =>
      withLocalSnsTopic { progressTopic =>
        withLocalSnsTopic { outgoingTopic =>
          withLocalS3Bucket { sourceBucket =>
            withLocalS3Bucket { destinationBucket =>
              val dstRootPath = "storage-root"
              withBagReplicator(
                queuePair,
                progressTopic,
                outgoingTopic,
                destinationBucket,
                dstRootPath)({ _ =>
                testWith(
                  (
                    sourceBucket,
                    queuePair,
                    destinationBucket,
                    dstRootPath,
                    progressTopic,
                    outgoingTopic))
              })
            }
          }
        }
      }
    }
  }

  def verifyBagCopied(src: BagLocation, dst: BagLocation): Assertion = {
    val sourceItems = getObjectSummaries(src)
    val sourceKeyEtags = sourceItems.map { _.getETag }

    val destinationItems = getObjectSummaries(dst)
    val destinationKeyEtags = destinationItems.map { _.getETag }

    destinationKeyEtags should contain theSameElementsAs sourceKeyEtags
  }

  private def getObjectSummaries(
    bagLocation: BagLocation): List[S3ObjectSummary] =
    s3Client
      .listObjects(bagLocation.storageNamespace, bagLocation.completePath)
      .getObjectSummaries
      .asScala
      .toList

}
