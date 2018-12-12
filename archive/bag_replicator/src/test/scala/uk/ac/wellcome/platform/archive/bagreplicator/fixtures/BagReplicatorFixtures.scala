package uk.ac.wellcome.platform.archive.bagreplicator.fixtures

import java.util.UUID

import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.bagreplicator.BagReplicator
import uk.ac.wellcome.platform.archive.bagreplicator.config.BagReplicatorConfig
import uk.ac.wellcome.platform.archive.bagreplicator.models.StorageLocation
import uk.ac.wellcome.platform.archive.common.fixtures.{ArchiveMessaging, BagLocationFixtures, RandomThings}
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

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
      withBag(storageBucket, bagInfo = bagInfo) { bagLocation =>
        val archiveComplete = ArchiveComplete(
          archiveRequestId = archiveRequestId,
          space = storageSpace,
          bagLocation = bagLocation
        )

        sendNotificationToSQS(
          queuePair.queue,
          archiveComplete
        )
        testWith(bagLocation)
      }

    def withApp[R](queuePair: QueuePair,
                   progressTopic: Topic,
                   destinationBucket: Bucket)(testWith: TestWith[BagReplicator, R]): R =
      withActorSystem { implicit actorSystem =>
        withMetricsSender(actorSystem) { metricsSender =>
          withArchiveMessageStream[NotificationMessage, Unit, R](queuePair.queue, metricsSender) { messageStream =>

            val bagReplicator = new BagReplicator(
              s3Client = s3Client,
              snsClient = snsClient,
              messageStream = messageStream,
              bagReplicatorConfig =
                BagReplicatorConfig(parallelism = 10,
                StorageLocation(destinationBucket.name, "storage-root")),
              snsProgressConfig = createSNSConfigWith(progressTopic)
            )

            bagReplicator.run()

            testWith(bagReplicator)
          }
        }
      }

    def withBagReplicator[R](
                          testWith: TestWith[(Bucket, QueuePair, Bucket, Topic), R]): R = {
      withLocalSqsQueueAndDlqAndTimeout(15) { queuePair =>
        withLocalSnsTopic { progressTopic =>
          withLocalS3Bucket { sourceBucket =>
            withLocalS3Bucket { destinationBucket =>
              withApp(queuePair, progressTopic, destinationBucket) { _ =>
                testWith((sourceBucket, queuePair, destinationBucket, progressTopic))
              }
            }
          }
        }
      }
    }
  }
