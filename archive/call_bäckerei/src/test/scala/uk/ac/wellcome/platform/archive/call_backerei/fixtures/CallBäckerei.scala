package uk.ac.wellcome.platform.archive.call_backerei.fixtures

import java.net.URI
import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, BagLocation, BagPath, DigitisedStorageType}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.call_backerei.modules.{ConfigModule, TestAppConfigModule, VHSModule}
import uk.ac.wellcome.platform.archive.call_backerei.{CallBäckerei => RegistrarApp}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, LocalVersionedHybridStore, S3}
import uk.ac.wellcome.test.fixtures.TestWith

trait CallBäckerei
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

  def withApp[R](queue: Queue, topic: Topic)(testWith: TestWith[RegistrarApp, R]): R = {

    class TestApp extends Logging {

      val appConfigModule = new TestAppConfigModule(
        queue = queue,
        topic = topic
      )

      val injector: Injector = Guice.createInjector(
        appConfigModule,
        ConfigModule,
        AkkaModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule,
        MessageStreamModule
      )

      val app = injector.getInstance(classOf[RegistrarApp])

    }

    testWith((new TestApp()).app)
  }

  def withCallBäckerei[R](
    testWith: TestWith[(QueuePair, Topic, RegistrarApp), R]): R = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { topic =>
        withApp(queue = queuePair.queue, topic = topic) { app =>
          testWith((queuePair, topic, app))
        }
      }
    })
  }
}
