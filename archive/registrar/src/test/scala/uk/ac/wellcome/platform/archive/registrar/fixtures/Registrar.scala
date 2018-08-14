package uk.ac.wellcome.platform.archive.registrar.fixtures

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.{AkkaS3, BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.{BagArchiveCompleteNotification, BagLocation, BagName}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.registrar.{Registrar => RegistrarApp}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import uk.ac.wellcome.json.JsonUtil._

trait Registrar extends AkkaS3 with Messaging with BagIt {

  def sendNotification(bagLocation: BagLocation, queuePair: QueuePair) =
    sendNotificationToSQS(
      queuePair.queue,
      BagArchiveCompleteNotification(bagLocation)
    )

  def withBagNotification[R](queuePair: QueuePair, storageBucket: Bucket, dataFileCount: Int = 1, valid: Boolean = true)(
    testWith: TestWith[BagLocation, R]) = {
    withBag(storageBucket, dataFileCount, valid) { bagLocation =>
      sendNotification(bagLocation, queuePair)

      testWith(bagLocation)
    }
  }

  def withBag[R](storageBucket: Bucket, dataFileCount: Int = 1, valid: Boolean = true)(
    testWith: TestWith[BagLocation, R]) = {
    val bagName = BagName(randomAlphanumeric())

    info(s"Creating bag $bagName")

    val fileEntries = createBag(bagName, dataFileCount, valid)
    val storagePrefix = "archive"

    val bagLocation = BagLocation(storageBucket.name, storagePrefix, bagName)

    fileEntries.map((entry: FileEntry) => {
      List(bagLocation.storagePath, entry.name).mkString("/")
      s3Client.putObject(bagLocation.storageNamespace, entry.name, entry.contents)
    })

    testWith(bagLocation)
  }

  def withApp[R](storageBucket: Bucket, queuePair: QueuePair, topicArn: Topic)(
    testWith: TestWith[RegistrarApp, R]) = {
    val registrar = new RegistrarApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          storageBucket.name,
          topicArn.arn),
        ConfigModule,
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule
      )
    }
    testWith(registrar)
  }

  def withRegistrar[R](
                        testWith: TestWith[(Bucket, QueuePair, Topic, RegistrarApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { snsTopic =>
        withLocalS3Bucket { storageBucket =>
          withApp(storageBucket, queuePair, snsTopic) { registrar =>
            testWith(
              (storageBucket, queuePair, snsTopic, registrar))
          }
        }
      }
    })
  }
}
