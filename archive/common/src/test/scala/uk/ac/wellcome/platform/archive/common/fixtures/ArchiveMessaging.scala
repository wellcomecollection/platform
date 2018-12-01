package uk.ac.wellcome.platform.archive.common.fixtures

import akka.actor.ActorSystem
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.test.fixtures.TestWith

trait ArchiveMessaging extends SQS {
  def withArchiveMessageStream[I, O, R](queue: Queue, metricsSender: MetricsSender)(testWith: TestWith[MessageStream[I, O], R])(implicit actorSystem: ActorSystem): R = {
    val messageStream = new MessageStream[I, O](
      sqsClient = asyncSqsClient,
      sqsConfig = createSQSConfigWith(queue),
      metricsSender = metricsSender
    )

    testWith(messageStream)
  }

  def withArchiveMessageStream[I, O, R](queue: Queue)(testWith: TestWith[MessageStream[I, O], R])(implicit actorSystem: ActorSystem): R =
    withMockMetricSender { mockMetricsSender =>
      withArchiveMessageStream[I, O, R](queue, metricsSender = mockMetricsSender) { messageStream =>
        testWith(messageStream)
      }
    }
}
