package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import org.mockito.Mockito.{times, verify}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class MergerWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with SQS
    with Akka
    with ExtendedPatience
    with MetricsSenderFixture
    with LocalVersionedHybridStore
    with SNS
    with Messaging
    with MergerTestUtils {
  case class TestObject(something: String)

  it(
    "reads matcher result messages, retrieves the works from vhs and sends them to sns") {

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, _) =>
        val recorderWorkEntry1 = createRecorderWorkEntryWith(version = 1)
        val recorderWorkEntry2 = createRecorderWorkEntryWith(version = 1)
        val recorderWorkEntry3 = createRecorderWorkEntryWith(version = 1)

        val matcherResult = matcherResultWith(
          Set(
            Set(recorderWorkEntry3),
            Set(recorderWorkEntry1, recorderWorkEntry2)))

        whenReady(
          storeInVHS(
            vhs,
            List(recorderWorkEntry1, recorderWorkEntry2, recorderWorkEntry3))) {
          _ =>
            sendNotificationToSQS(
              queue = queue,
              message = matcherResult
            )

            eventually {
              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)

              val worksSent = getMessages[TransformedBaseWork](topic)
              worksSent should contain only (recorderWorkEntry1.work,
              recorderWorkEntry2.work,
              recorderWorkEntry3.work)
            }
        }
    }
  }

  it("sends InvisibleWorks unmerged") {

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, _) =>
        val recorderWorkEntry = RecorderWorkEntry(
          work = createUnidentifiedInvisibleWork
        )

        val matcherResult = matcherResultWith(Set(Set(recorderWorkEntry)))

        whenReady(storeInVHS(vhs, recorderWorkEntry)) { _ =>
          sendNotificationToSQS(
            queue = queue,
            message = matcherResult
          )

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)

            val worksSent = getMessages[TransformedBaseWork](topic)
            worksSent should contain only recorderWorkEntry.work
          }
        }
    }
  }

  it("fails if the work is not in vhs") {

    withMergerWorkerServiceFixtures {
      case (_, QueuePair(queue, dlq), topic, metricsSender) =>
        val recorderWorkEntry = createRecorderWorkEntryWith(version = 1)

        val matcherResult = matcherResultWith(Set(Set(recorderWorkEntry)))

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          listMessagesReceivedFromSNS(topic) shouldBe empty
          verify(metricsSender, times(3))
            .incrementCount(org.mockito.Matchers.endsWith("_failure"))
        }
    }
  }

  it("discards works with newer versions in vhs, sends along the others") {

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, _) =>
        val recorderWorkEntry = createRecorderWorkEntryWith(version = 1)
        val work = createUnidentifiedWorkWith(version = 1)
        val olderVersionRecorderWorkEntry = RecorderWorkEntry(work = work)
        val newerVersionRecorderWorkEntry =
          RecorderWorkEntry(work = work.copy(version = 2))

        val matcherResult = matcherResultWith(
          Set(Set(recorderWorkEntry, olderVersionRecorderWorkEntry)))

        whenReady(
          storeInVHS(
            vhs,
            List(recorderWorkEntry, newerVersionRecorderWorkEntry))) { _ =>
          sendNotificationToSQS(
            queue = queue,
            message = matcherResult
          )

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
            val worksSent = getMessages[TransformedBaseWork](topic)
            worksSent should contain only recorderWorkEntry.work
          }
        }
    }
  }

  it("discards works with version 0 and sends along the others") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, _) =>
        val versionZeroWork: RecorderWorkEntry =
          createRecorderWorkEntryWith(version = 0)
        val recorderWorkEntry = versionZeroWork.copy(
          work = versionZeroWork.work
            .asInstanceOf[UnidentifiedWork]
            .copy(version = 1)
        )

        val matcherResult =
          matcherResultWith(Set(Set(recorderWorkEntry, versionZeroWork)))

        whenReady(storeInVHS(vhs, recorderWorkEntry)) { _ =>
          sendNotificationToSQS(
            queue = queue,
            message = matcherResult
          )

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)

            val worksSent = getMessages[TransformedBaseWork](topic)
            worksSent should contain only recorderWorkEntry.work
          }
        }
    }
  }

  it("fails if the message sent is not a matcher result") {
    withMergerWorkerServiceFixtures {
      case (_, QueuePair(queue, dlq), _, metricsSender) =>
        sendNotificationToSQS(
          queue = queue,
          message = TestObject("lallabalula")
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          verify(metricsSender, times(3))
            .incrementCount(org.mockito.Matchers.endsWith("_gracefulFailure"))
        }
    }
  }

  def withMergerWorkerServiceFixtures[R](
    testWith: TestWith[(VersionedHybridStore[RecorderWorkEntry,
                                             EmptyMetadata,
                                             ObjectStore[RecorderWorkEntry]],
                        QueuePair,
                        Topic,
                        MetricsSender),
                       R]): R = {
    withLocalS3Bucket { storageBucket =>
      withLocalS3Bucket { messageBucket =>
        withLocalDynamoDbTable { table =>
          withTypeVHS[RecorderWorkEntry, EmptyMetadata, R](storageBucket, table) {
            vhs =>
              withActorSystem { actorSystem =>
                withLocalSqsQueueAndDlq {
                  case queuePair @ QueuePair(queue, dlq) =>
                    withLocalSnsTopic { topic =>
                      withMockMetricSender { metricsSender =>
                        withSQSStream[NotificationMessage, R](
                          actorSystem,
                          queue,
                          metricsSender) { sqsStream =>
                          withMessageWriter[TransformedBaseWork, R](
                            messageBucket,
                            topic) { snsWriter =>
                            withMergerWorkerService(
                              actorSystem,
                              sqsStream,
                              vhs,
                              snsWriter) { _ =>
                              testWith((vhs, queuePair, topic, metricsSender))
                            }
                          }
                        }
                      }
                    }
                }
              }
          }
        }
      }
    }
  }

  def withMergerWorkerService[R](
    actorSystem: ActorSystem,
    sqsStream: SQSStream[NotificationMessage],
    vhs: VersionedHybridStore[RecorderWorkEntry,
                              EmptyMetadata,
                              ObjectStore[RecorderWorkEntry]],
    messageWriter: MessageWriter[TransformedBaseWork])(
    testWith: TestWith[MergerWorkerService, R]) = {
    testWith(
      new MergerWorkerService(actorSystem, sqsStream, vhs, messageWriter))
  }
}
