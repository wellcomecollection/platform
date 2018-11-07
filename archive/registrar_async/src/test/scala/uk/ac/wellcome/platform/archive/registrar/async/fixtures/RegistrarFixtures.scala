package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.registrar.async.Registrar
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.fixtures.{
  LocalDynamoDb,
  LocalVersionedHybridStore,
  S3
}
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream

trait RegistrarFixtures
    extends S3
    with Messaging
    with LocalVersionedHybridStore
    with BagLocationFixtures
    with LocalDynamoDb {

  def sendNotification(requestId: UUID,
                       storageSpace: StorageSpace,
                       bagLocation: BagLocation,
                       queuePair: QueuePair) =
    sendNotificationToSQS(
      queuePair.queue,
      ArchiveComplete(requestId, storageSpace, bagLocation)
    )

  def withBagNotification[R](requestId: UUID,
                             queuePair: QueuePair,
                             storageBucket: Bucket,
                             dataFileCount: Int = 1)(
    testWith: TestWith[(BagLocation, BagInfo, BagId), R]) = {
    withBag(storageBucket, dataFileCount) {
      case (bagLocation, bagInfo, bagId) =>
        sendNotification(requestId, bagId.space, bagLocation, queuePair)
        testWith((bagLocation, bagInfo, bagId))
    }
  }

  override def createTable(table: Table) = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(
          new KeySchemaElement()
            .withAttributeName("id")
            .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("id")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
    )

    table
  }

  def withApp[R](hybridStoreBucket: Bucket,
                 hybridStoreTable: Table,
                 queuePair: QueuePair,
                 progressTopic: Topic)(testWith: TestWith[Registrar, R]): R =
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        val messageStream = new MessageStream[NotificationMessage, Unit](
          actorSystem = actorSystem,
          sqsClient = asyncSqsClient,
          sqsConfig = SQSConfig(queueUrl = queuePair.queue.url),
          metricsSender = metricsSender
        )
        withTypeVHS[StorageManifest, EmptyMetadata, R](
          bucket = hybridStoreBucket,
          table = hybridStoreTable
        ) { dataStore =>
          val registrar = new Registrar(
            snsClient = snsClient,
            progressSnsConfig = SNSConfig(topicArn = progressTopic.arn),
            s3Client = s3Client,
            messageStream = messageStream,
            dataStore = dataStore,
            actorSystem = actorSystem
          )

          testWith(registrar)
        }
      }
    }

  type ManifestVHS = VersionedHybridStore[StorageManifest,
                                          EmptyMetadata,
                                          ObjectStore[StorageManifest]]

  def withRegistrar[R](testWith: TestWith[
    (Bucket, QueuePair, Topic, Registrar, ManifestVHS),
    R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic {
        progressTopic =>
          withLocalS3Bucket {
            storageBucket =>
              withLocalS3Bucket {
                hybridStoreBucket =>
                  withLocalDynamoDbTable {
                    hybridDynamoTable =>
                      withApp(
                        hybridStoreBucket,
                        hybridDynamoTable,
                        queuePair,
                        progressTopic) { registrar =>
                        implicit val storageBackend =
                          new S3StorageBackend(s3Client)

                        withTypeVHS[StorageManifest, EmptyMetadata, R](
                          hybridStoreBucket,
                          hybridDynamoTable) { vhs =>
                          testWith(
                            (
                              storageBucket,
                              queuePair,
                              progressTopic,
                              registrar,
                              vhs)
                          )
                        }
                      }
                  }
              }

          }

      }
    })
  }

  def withRegistrarAndBrokenVHS[R](
    testWith: TestWith[(Bucket, QueuePair, Topic, Registrar, Bucket), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic {
        progressTopic =>
          withLocalS3Bucket { storageBucket =>
            withLocalS3Bucket { hybridStoreBucket =>
              withApp(
                hybridStoreBucket,
                Table("does-not-exist", ""),
                queuePair,
                progressTopic) { registrar =>
                testWith(
                  (
                    storageBucket,
                    queuePair,
                    progressTopic,
                    registrar,
                    hybridStoreBucket)
                )
              }
            }
          }

      }
    })
  }

}
