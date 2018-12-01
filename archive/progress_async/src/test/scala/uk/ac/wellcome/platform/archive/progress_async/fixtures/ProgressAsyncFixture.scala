package uk.ac.wellcome.platform.archive.progress_async.fixtures

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressTrackerFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.ProgressAsync
import uk.ac.wellcome.platform.archive.progress_async.flows.ProgressUpdateFlow
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
    with Messaging
    with ScalaFutures {

  def withProgress[R](monitor: ProgressTracker)(
    testWith: TestWith[Progress, R]) = {
    val createdProgress = createProgress

    whenReady(monitor.initialise(createdProgress)) { storedProgress =>
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

  def withApp[R](queuePair: QueuePair, topic: Topic, table: Table)(
    testWith: TestWith[ProgressAsync, R]): R =
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withMetricsSender(actorSystem) { metricsSender =>
          val progressAsync = new ProgressAsync(
            messageStream = new MessageStream[NotificationMessage, Unit](
              sqsClient = asyncSqsClient,
              sqsConfig = createSQSConfigWith(queuePair.queue),
              metricsSender = metricsSender
            ),
            progressTracker = new ProgressTracker(
              dynamoClient = dynamoDbClient,
              dynamoConfig = createDynamoConfigWith(table)
            ),
            snsClient = snsClient,
            snsConfig = createSNSConfigWith(topic)
          )

          testWith(progressAsync)
        }
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(QueuePair, Topic, Table, ProgressAsync), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15) { qPair =>
      withLocalSnsTopic { topic =>
        withProgressTrackerTable { table =>
          withApp(qPair, topic, table) { progressAsync =>
            testWith((qPair, topic, table, progressAsync))
          }
        }
      }
    }
  }
}
