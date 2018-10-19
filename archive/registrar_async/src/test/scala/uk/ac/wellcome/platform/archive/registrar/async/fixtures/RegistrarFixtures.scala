package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagId,
  BagLocation
}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.async.Registrar
import uk.ac.wellcome.platform.archive.registrar.async.modules.{
  ConfigModule,
  TestAppConfigModule
}
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.platform.archive.registrar.common.modules.VHSModule
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

trait RegistrarFixtures
    extends S3
    with Messaging
    with LocalVersionedHybridStore
    with BagLocationFixtures
    with LocalDynamoDb {

  def sendNotification(requestId: UUID,
                       bagId: BagId,
                       bagLocation: BagLocation,
                       queuePair: QueuePair) =
    sendNotificationToSQS(
      queuePair.queue,
      ArchiveComplete(requestId, bagId, bagLocation)
    )

  def withBagNotification[R](
    requestId: UUID,
    bagId: BagId,
    queuePair: QueuePair,
    storageBucket: Bucket,
    dataFileCount: Int = 1)(testWith: TestWith[BagLocation, R]) = {
    withBag(storageBucket, dataFileCount) { bagLocation =>
      sendNotification(requestId, bagId, bagLocation, queuePair)
      testWith(bagLocation)
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

  def withApp[R](storageBucket: Bucket,
                 hybridStoreBucket: Bucket,
                 hybridStoreTable: Table,
                 queuePair: QueuePair,
                 progressTopic: Topic)(testWith: TestWith[Registrar, R]) = {

    class TestApp extends Logging {

      val appConfigModule = new TestAppConfigModule(
        queuePair.queue.url,
        storageBucket.name,
        progressTopic.arn,
        hybridStoreTable.name,
        hybridStoreBucket.name,
        "archive"
      )

      val injector: Injector = Guice.createInjector(
        appConfigModule,
        ConfigModule,
        VHSModule,
        AkkaModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSClientModule,
        S3ClientModule,
        DynamoClientModule,
        MessageStreamModule
      )

      val app = injector.getInstance(classOf[Registrar])

    }

    testWith((new TestApp()).app)
  }

  type ManifestVHS = VersionedHybridStore[StorageManifest,
                                          EmptyMetadata,
                                          ObjectStore[StorageManifest]]

  def withRegistrar[R](
    testWith: TestWith[
      (Bucket, QueuePair, Topic, Registrar, ManifestVHS),
      R]) = {
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
                            storageBucket,
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
    testWith: TestWith[(Bucket, QueuePair, Topic, Registrar, Bucket),
                       R]) = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
          withLocalSnsTopic {
            progressTopic =>
              withLocalS3Bucket {
                storageBucket =>
                  withLocalS3Bucket { hybridStoreBucket =>
                    withApp(
                      storageBucket,
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
