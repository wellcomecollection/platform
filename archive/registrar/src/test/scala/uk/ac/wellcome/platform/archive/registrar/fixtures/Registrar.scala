package uk.ac.wellcome.platform.archive.registrar.fixtures

import java.net.URI
import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath,
  DigitisedStorageType
}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.registrar.modules.{
  ConfigModule,
  TestAppConfigModule,
  VHSModule
}
import uk.ac.wellcome.platform.archive.registrar.{Registrar => RegistrarApp}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.fixtures.{
  LocalDynamoDb,
  LocalVersionedHybridStore,
  S3
}
import uk.ac.wellcome.test.fixtures.TestWith

trait Registrar
    extends S3
    with Messaging
    with LocalVersionedHybridStore
    with BagIt
    with ProgressMonitorFixture
    with LocalDynamoDb {

  def sendNotification(requestId: UUID,
                       bagLocation: BagLocation,
                       callbackUrl: Option[URI],
                       queuePair: QueuePair) =
    sendNotificationToSQS(
      queuePair.queue,
      ArchiveComplete(requestId, bagLocation, callbackUrl)
    )

  def withBagNotification[R](
    requestId: UUID,
    callbackUrl: Option[URI],
    queuePair: QueuePair,
    storageBucket: Bucket,
    dataFileCount: Int = 1)(testWith: TestWith[BagLocation, R]) = {
    withBag(storageBucket, dataFileCount) { bagLocation =>
      sendNotification(requestId, bagLocation, callbackUrl, queuePair)
      testWith(bagLocation)
    }
  }

  def withBag[R](storageBucket: Bucket, dataFileCount: Int = 1)(
    testWith: TestWith[BagLocation, R]) = {
    val bagIdentifier = randomAlphanumeric()

    info(s"Creating bag $bagIdentifier")

    val fileEntries = createBag(bagIdentifier, dataFileCount)
    val storagePrefix = "archive"

    val bagLocation = BagLocation(
      storageBucket.name,
      storagePrefix,
      BagPath(s"$DigitisedStorageType/$bagIdentifier"))

    fileEntries.map((entry: FileEntry) => {
      s3Client
        .putObject(
          bagLocation.storageNamespace,
          s"$storagePrefix/${bagLocation.bagPath}/${entry.name}",
          entry.contents
        )
    })

    testWith(bagLocation)
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
                 topicArn: Topic,
                 progressTable: Table)(testWith: TestWith[RegistrarApp, R]) = {

    class TestApp extends Logging {

      val appConfigModule = new TestAppConfigModule(
        queuePair.queue.url,
        storageBucket.name,
        topicArn.arn,
        hybridStoreTable.name,
        hybridStoreBucket.name,
        "archive",
        progressTable
      )

      val injector: Injector = Guice.createInjector(
        appConfigModule,
        ConfigModule,
        VHSModule,
        AkkaModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule,
        DynamoClientModule,
        ProgressMonitorModule,
        MessageStreamModule
      )

      val app = injector.getInstance(classOf[RegistrarApp])

    }

    testWith((new TestApp()).app)
  }

  def withRegistrar[R](
    testWith: TestWith[
      (Bucket, QueuePair, Topic, RegistrarApp, Bucket, Table, Table),
      R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic {
        snsTopic =>
          withLocalS3Bucket {
            storageBucket =>
              withLocalS3Bucket {
                hybridStoreBucket =>
                  withLocalDynamoDbTable {
                    hybridDynamoTable =>
                      withSpecifiedLocalDynamoDbTable(
                        createProgressMonitorTable) { progressTable =>
                        withApp(
                          storageBucket,
                          hybridStoreBucket,
                          hybridDynamoTable,
                          queuePair,
                          snsTopic,
                          progressTable) { registrar =>
                          testWith(
                            (
                              storageBucket,
                              queuePair,
                              snsTopic,
                              registrar,
                              hybridStoreBucket,
                              hybridDynamoTable,
                              progressTable)
                          )
                        }
                      }
                  }
              }
          }
      }
    })
  }
}
