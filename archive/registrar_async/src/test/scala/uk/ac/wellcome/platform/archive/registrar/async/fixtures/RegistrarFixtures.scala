package uk.ac.wellcome.platform.archive.registrar.async.fixtures

import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.generators.ArchiveCompleteGenerators
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.registrar.async.Registrar
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.registrar.fixtures.StorageManifestVHSFixture

trait RegistrarFixtures
    extends S3
    with Messaging
    with ArchiveCompleteGenerators
    with BagLocationFixtures
    with LocalDynamoDb
    with StorageManifestVHSFixture {

  def withBagNotification[R](
    queuePair: QueuePair,
    storageBucket: Bucket,
    archiveRequestId: UUID = randomUUID,
    storageSpace: StorageSpace = randomStorageSpace
  )(testWith: TestWith[(BagLocation, BagInfo), R]): R =
    withBag(storageBucket) {
      case (bagLocation, bagInfo) =>
        val archiveComplete = createArchiveCompleteWith(
          archiveRequestId = archiveRequestId,
          space = storageSpace,
          bagLocation = bagLocation
        )

        sendNotificationToSQS(
          queuePair.queue,
          archiveComplete
        )
        testWith((bagLocation, bagInfo))
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
          sqsConfig = createSQSConfigWith(queuePair.queue),
          metricsSender = metricsSender
        )
        withStorageManifestVHS(hybridStoreTable, hybridStoreBucket) {
          dataStore =>
            val registrar = new Registrar(
              snsClient = snsClient,
              progressSnsConfig = createSNSConfigWith(progressTopic),
              s3Client = s3Client,
              messageStream = messageStream,
              dataStore = dataStore,
              actorSystem = actorSystem
            )

            registrar.run()

            testWith(registrar)
        }
      }
    }

  def withRegistrar[R](
    testWith: TestWith[(Bucket, QueuePair, Topic, StorageManifestVHS), R])
    : R = {
    withLocalSqsQueueAndDlqAndTimeout(15) { queuePair =>
      withLocalSnsTopic { progressTopic =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { hybridStoreBucket =>
            withLocalDynamoDbTable { hybridDynamoTable =>
              withApp(
                hybridStoreBucket,
                hybridDynamoTable,
                queuePair,
                progressTopic) { _ =>
                withStorageManifestVHS(hybridDynamoTable, hybridStoreBucket) {
                  vhs =>
                    testWith(
                      (storageBucket, queuePair, progressTopic, vhs)
                    )
                }
              }
            }
          }
        }
      }
    }
  }

  def withRegistrarAndBrokenVHS[R](
    testWith: TestWith[(Bucket, QueuePair, Topic, Bucket), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic { progressTopic =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { hybridStoreBucket =>
            withApp(
              hybridStoreBucket,
              Table("does-not-exist", ""),
              queuePair,
              progressTopic) { _ =>
              testWith(
                (storageBucket, queuePair, progressTopic, hybridStoreBucket)
              )
            }
          }
        }

      }
    })
  }

}
