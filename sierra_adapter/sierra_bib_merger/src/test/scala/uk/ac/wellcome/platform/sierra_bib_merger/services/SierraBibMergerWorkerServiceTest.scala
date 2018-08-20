package uk.ac.wellcome.platform.sierra_bib_merger.services

import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with SQS
    with MetricsSenderFixture
    with Akka
    with S3
    with LocalVersionedHybridStore
    with IntegrationPatience
    with SierraUtil {

  it(
    "throws a GracefulFailureException if the message on the queue does not represent a SierraRecord") {
    withWorkerServiceFixtures {
      case (metricsSender, QueuePair(queue, dlq), _) =>
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

  def withWorkerServiceFixtures[R](
    testWith: TestWith[(MetricsSender, QueuePair, SierraBibMergerWorkerService),
                       R]) =
    withActorSystem { system =>
      withMockMetricSender { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withSQSStream[NotificationMessage, R](system, queue, metricsSender) {
              sqsStream =>
                withLocalDynamoDbTable { table =>
                  withLocalS3Bucket { storageBucket =>
                    withTypeVHS[SierraTransformable, SourceMetadata, R](
                      storageBucket,
                      table) { vhs =>
                      val mergerUpdaterService =
                        new SierraBibMergerUpdaterService(vhs)

                      val worker = new SierraBibMergerWorkerService(
                        system,
                        sqsStream = sqsStream,
                        mergerUpdaterService)
                      testWith((metricsSender, queuePair, worker))
                    }
                  }
                }
            }
        }
      }
    }
}
