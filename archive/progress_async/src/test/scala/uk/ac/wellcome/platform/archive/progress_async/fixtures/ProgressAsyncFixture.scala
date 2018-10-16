package uk.ac.wellcome.platform.archive.progress_async.fixtures

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressTrackerFixture
import uk.ac.wellcome.platform.archive.common.progress.models.progress.{Progress => ProgressModel}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.progress_async.{ProgressAsync => ProgressApp}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith

trait ProgressAsyncFixture
    extends S3
    with LocalDynamoDb
    with RandomThings
    with ProgressTrackerFixture
    with Messaging {

  def withProgress[R](monitor: ProgressTracker)(
    testWith: TestWith[ProgressModel, R]) = {
    val createdProgress = createProgress()
    val storedProgress = monitor.initialise(createdProgress)
    testWith(storedProgress)
  }

  def withApp[R](qPair: QueuePair, topic: Topic, table: Table)(
    testWith: TestWith[ProgressApp, R]) = {

    val progress = new ProgressApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          qPair.queue.url,
          topic.arn,
          table
        ),
        ConfigModule,
        AkkaModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSClientModule,
        ProgressTrackerModule
      )
    }
    testWith(progress)
  }

  def withConfiguredApp[R](
    testWith: TestWith[(QueuePair, Topic, Table, ProgressApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15) { qPair =>
      withLocalSnsTopic { topic =>
        withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
          withApp(qPair, topic, table) { progressAsync =>
            testWith((qPair, topic, table, progressAsync))
          }
        }
      }
    }
  }
}
