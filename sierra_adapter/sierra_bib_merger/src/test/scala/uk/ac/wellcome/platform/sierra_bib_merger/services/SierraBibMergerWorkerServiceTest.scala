package uk.ac.wellcome.platform.sierra_bib_merger.services

import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_bib_merger.fixtures.WorkerServiceFixture
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.fixtures.TestWith

class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with SQS
    with MetricsSenderFixture
    with S3
    with Messaging
    with LocalVersionedHybridStore
    with IntegrationPatience
    with SierraAdapterHelpers
    with SierraGenerators
    with WorkerServiceFixture {

  it(
    "records a failure if the message on the queue does not represent a SierraRecord") {
    withWorkerServiceFixtures {
      case (metricsSender, QueuePair(queue, dlq)) =>
        sendNotificationToSQS(
          queue = queue,
          body = "null"
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          verify(metricsSender, never()).incrementCount(
            "SierraBibMergerUpdaterService_ProcessMessage_failure")
        }
    }
  }

  private def withWorkerServiceFixtures[R](
    testWith: TestWith[(MetricsSender, QueuePair), R]) =
    withMockMetricSender { metricsSender =>
      withLocalSnsTopic { topic =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, _) =>
            withLocalDynamoDbTable { table =>
              withLocalS3Bucket { storageBucket =>
                withWorkerService(storageBucket, table, queue, topic) { _ =>
                  testWith((metricsSender, queuePair))
                }
              }
            }
        }
      }
    }

}
