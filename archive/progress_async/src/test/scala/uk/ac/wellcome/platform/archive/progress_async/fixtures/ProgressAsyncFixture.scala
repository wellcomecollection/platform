package uk.ac.wellcome.platform.archive.progress_async.fixtures

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.google.inject.Guice
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressTrackerFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressUpdate}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.flows.ProgressUpdateFlow
import uk.ac.wellcome.platform.archive.progress_async.modules.{ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.platform.archive.progress_async.{ProgressAsync => ProgressApp}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith
import scala.concurrent.ExecutionContext.Implicits.global

trait ProgressAsyncFixture
    extends S3
    with LocalDynamoDb
    with RandomThings
    with ProgressTrackerFixture
    with Messaging with ScalaFutures{

  def withProgress[R](monitor: ProgressTracker)(
    testWith: TestWith[Progress, R]) = {
    val createdProgress = createProgress

    whenReady(monitor.initialise(createdProgress)) { storedProgress=>

      testWith(storedProgress)
    }
  }

  def withProgressUpdateFlow[R](table: Table)(
    testWith: TestWith[(
                         Flow[ProgressUpdate, Progress, NotUsed],
                         ProgressTracker
                       ),
                       R]): R = {

    val progressTracker = new ProgressTracker(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith((ProgressUpdateFlow(progressTracker), progressTracker))
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
