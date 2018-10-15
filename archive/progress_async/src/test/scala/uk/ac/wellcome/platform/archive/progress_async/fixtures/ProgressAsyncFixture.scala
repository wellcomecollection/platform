package uk.ac.wellcome.platform.archive.progress_async.fixtures

import java.util.UUID

import com.google.inject.Guice
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ProgressEvent,
  ProgressUpdate,
  Progress => ProgressModel
}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.progress_async.modules.{
  ConfigModule,
  TestAppConfigModule
}
import uk.ac.wellcome.platform.archive.progress_async.{
  ProgressAsync => ProgressApp
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith

trait ProgressAsyncFixture
    extends S3
    with LocalDynamoDb
    with RandomThings
    with ProgressMonitorFixture
    with Messaging {

  import ProgressModel._

  def withProgress[R](monitor: ProgressMonitor)(
    testWith: TestWith[models.Progress, R]) = {
    val id = randomUUID

    val createdProgress =
      ProgressModel(id, uploadUri, Some(callbackUri), space)

    val storedProgress = monitor.create(createdProgress)

    testWith(storedProgress)
  }

  def withProgressUpdate[R](id: UUID, status: Status = None)(
    testWith: TestWith[ProgressUpdate, R]) = {

    val event = ProgressEvent(
      description = randomAlphanumeric()
    )

    val progress = ProgressUpdate(
      id = id,
      events = List(event),
      status = status
    )

    testWith(progress)
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
        ProgressMonitorModule
      )
    }
    testWith(progress)
  }

  def withConfiguredApp[R](
    testWith: TestWith[(QueuePair, Topic, Table, ProgressApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15) { qPair =>
      withLocalSnsTopic { topic =>
        withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
          withApp(qPair, topic, table) { progressAsync =>
            testWith((qPair, topic, table, progressAsync))
          }
        }
      }
    }
  }
}
