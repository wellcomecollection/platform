package uk.ac.wellcome.platform.sierra_bib_merger.services

import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.storage.test.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

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
    with ExtendedPatience {

  it(
    "throws a GracefulFailureException if the message on the queue does not represent a SierraRecord") {

    withWorkerServiceFixtures {
      case (metricsSender, QueuePair(queue, dlq), _) =>
        sqsClient.sendMessage(
          queue.url,
          toJson(
            NotificationMessage(
              Subject = "default-subject",
              Message = "null",
              TopicArn = "",
              MessageId = "")).get)

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          verify(metricsSender, never()).incrementCount(
            "SierraBibMergerUpdaterService_MessageProcessingFailure",
            1.0)
        }
    }
  }

  def withWorkerServiceFixtures[R](
    testWith: TestWith[
      (MetricsSender, QueuePair, SierraBibMergerWorkerService),
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
                      val sqsToDynamoStream =
                        new SQSToDynamoStream[SierraRecord](system, sqsStream)

                      val mergerUpdaterService =
                        new SierraBibMergerUpdaterService(vhs, metricsSender)

                      val worker = new SierraBibMergerWorkerService(
                        system,
                        sqsToDynamoStream,
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
