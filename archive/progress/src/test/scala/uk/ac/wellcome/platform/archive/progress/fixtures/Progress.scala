package uk.ac.wellcome.platform.archive.progress.fixtures

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.{AkkaS3, BagIt}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.progress.modules.{
  ConfigModule,
  TestAppConfigModule
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.platform.archive.progress.{Progress => ProgressApp}

trait Progress
    extends AkkaS3
    with LocalDynamoDb
    with ProgressMonitorFixture
    with Messaging
    with BagIt {

  def withApp[R](queuePair: QueuePair, topicArn: Topic, progressTable: Table)(
    testWith: TestWith[ProgressApp, R]) = {
    val progress = new ProgressApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          topicArn.arn,
          progressTable
        ),
        ConfigModule,
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule,
        ProgressMonitorModule
      )
    }
    testWith(progress)
  }

  def withProgress[R](
    testWith: TestWith[(QueuePair, Topic, Table, ProgressApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { snsTopic =>
        withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) {
          progressTable =>
            withApp(queuePair, snsTopic, progressTable) { archivist =>
              testWith((queuePair, snsTopic, progressTable, archivist))
            }

        }
      }
    })
  }
}
