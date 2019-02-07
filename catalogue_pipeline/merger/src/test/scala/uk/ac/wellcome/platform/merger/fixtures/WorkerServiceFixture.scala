package uk.ac.wellcome.platform.merger.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal.BaseWork
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.merger.services._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends LocalWorksVhs
    with Akka
    with Messaging
    with S3 {
  def withWorkerService[R](vhs: TransformedBaseWorkVHS,
                           topic: Topic,
                           queue: Queue,
                           metricsSender: MetricsSender)(
    testWith: TestWith[MergerWorkerService, R]): R =
    withLocalS3Bucket { messageBucket =>
      withMessageWriter[BaseWork, R](messageBucket, topic) { messageWriter =>
        withActorSystem { implicit actorSystem =>
          withSQSStream[NotificationMessage, R](
            queue = queue,
            metricsSender = metricsSender) { sqsStream =>
            val workerService = new MergerWorkerService(
              sqsStream = sqsStream,
              playbackService = new RecorderPlaybackService(vhs),
              mergerManager = new MergerManager(PlatformMerger),
              messageWriter = messageWriter
            )

            workerService.run()

            testWith(workerService)
          }
        }
      }
    }

  def withWorkerService[R](
    vhs: TransformedBaseWorkVHS,
    topic: Topic,
    queue: Queue)(testWith: TestWith[MergerWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withWorkerService(vhs, topic, queue, metricsSender) { workerService =>
          testWith(workerService)
        }
      }
    }
}
