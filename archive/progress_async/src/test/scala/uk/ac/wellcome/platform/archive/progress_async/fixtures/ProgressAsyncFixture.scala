package uk.ac.wellcome.platform.archive.progress_async.fixtures

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.fixtures.{
  ArchiveMessaging,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressTrackerFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.ProgressAsync
import uk.ac.wellcome.platform.archive.progress_async.flows.ProgressUpdateFlow
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith

trait ProgressAsyncFixture
    extends S3
    with LocalDynamoDb
    with RandomThings
    with ArchiveMessaging
    with ProgressTrackerFixture
    with Messaging
    with ScalaFutures {

  def withProgress[R](monitor: ProgressTracker)(
    testWith: TestWith[Progress, R]): R = {
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
                       R]): R =
    withProgressTracker(table) { progressTracker =>
      testWith((ProgressUpdateFlow(progressTracker), progressTracker))
    }

  def withApp[R](queuePair: QueuePair, topic: Topic, table: Table)(
    testWith: TestWith[ProgressAsync, R]): R =
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withMetricsSender(actorSystem) { metricsSender =>
          withArchiveMessageStream[NotificationMessage, Unit, R](
            queuePair.queue,
            metricsSender) { messageStream =>
            withProgressTracker(table) { progressTracker =>
              val progressAsync = new ProgressAsync(
                messageStream = messageStream,
                progressTracker = progressTracker,
                snsClient = snsClient,
                snsConfig = createSNSConfigWith(topic)
              )

              testWith(progressAsync)
            }
          }
        }
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(QueuePair, Topic, Table, ProgressAsync), R]): R = {
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
