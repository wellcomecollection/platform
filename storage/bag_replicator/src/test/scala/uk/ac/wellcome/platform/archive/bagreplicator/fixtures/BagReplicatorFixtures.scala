package uk.ac.wellcome.platform.archive.bagreplicator.fixtures

import java.util.UUID

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
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.collection.JavaConverters._

trait BagReplicatorFixtures
    extends S3
    with RandomThings
    with Messaging
    with Akka
    with BagLocationFixtures
    with ArchiveMessaging {

  def verifyBagCopied(
    sourceLocation: BagLocation,
    storageDestination: ReplicatorDestinationConfig
  ): Assertion = {
    val sourceItems = s3Client.listObjects(
      sourceLocation.storageNamespace,
      sourceLocation.completePath)

    val sourceKeyEtags =
      sourceItems.getObjectSummaries.asScala.toList.map(_.getETag)

//    val bagPath = List(
//      storageDestination.rootPath,
//      sourceLocation.bagPath
//    ).mkString("/")

    val destinationItems = s3Client.listObjects(
      storageDestination.namespace
    )

    val destinationKeyEtags =
      destinationItems.getObjectSummaries.asScala.toList.map(_.getETag)

    destinationKeyEtags should contain theSameElementsAs sourceKeyEtags
  }

  def withBagNotification[R](
    queuePair: QueuePair,
    storageBucket: Bucket,
    archiveRequestId: UUID = randomUUID,
    storageSpace: StorageSpace = randomStorageSpace,
    bagInfo: BagInfo = randomBagInfo
  )(testWith: TestWith[BagLocation, R]): R =
    withBag(storageBucket, bagInfo = bagInfo, storageSpace = storageSpace) {
      bagLocation =>
        val archiveComplete = ArchiveComplete(
          archiveRequestId = archiveRequestId,
          bagLocation = bagLocation
        )

        sendNotificationToSQS(
          queuePair.queue,
          archiveComplete
        )

        testWith(bagLocation)
    }

  def withBagReplicator[R](
    queuePair: QueuePair,
    progressTopic: Topic,
    outgoingTopic: Topic,
    destinationBucket: Bucket)(testWith: TestWith[BagReplicator, R]): R =
    withActorSystem { implicit actorSystem =>
      withArchiveMessageStream[NotificationMessage, Unit, R](queuePair.queue) {
        messageStream =>
          val bagReplicator = new BagReplicator(
            s3Client = s3Client,
            snsClient = snsClient,
            messageStream = messageStream,
            bagReplicatorConfig = BagReplicatorConfig(
              parallelism = 10,
              ReplicatorDestinationConfig(
                destinationBucket.name,
                "storage-root")),
            progressSnsConfig = createSNSConfigWith(progressTopic),
            outgoingSnsConfig = createSNSConfigWith(outgoingTopic)
          )

          bagReplicator.run()

          testWith(bagReplicator)
      }
    }

  def withApp[R](
    testWith: TestWith[(Bucket, QueuePair, Bucket, Topic, Topic), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(15) { queuePair =>
      withLocalSnsTopic { progressTopic =>
        withLocalSnsTopic { outgoingTopic =>
          withLocalS3Bucket { sourceBucket =>
            withLocalS3Bucket { destinationBucket =>
              withBagReplicator(
                queuePair,
                progressTopic,
                outgoingTopic,
                destinationBucket) { _ =>
                testWith(
                  (
                    sourceBucket,
                    queuePair,
                    destinationBucket,
                    progressTopic,
                    outgoingTopic))
              }
            }
          }
        }
      }
    }
  }
}
