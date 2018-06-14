package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.ExecutionContext.Implicits.global

class MergerWorkerServiceTest extends FunSpec with SQS with Akka with ExtendedPatience with MetricsSenderFixture with LocalVersionedHybridStore with SNS with JsonTestUtil{
  case class TestObject(something: String)

  it("reads matcher result messages, retrieves the works from vhs and sends them to sns") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[RecorderWorkEntry, EmptyMetadata, Assertion](bucket, table) { vhs =>
          withMergerWorkerServiceFixtures { case (QueuePair(queue, dlq), topic, _) =>
            val unidentifiedWork = UnidentifiedWork(title = Some("dfmsng"), SourceIdentifier(IdentifierType("sierra-system-number"), "Work", "b123456"), version = 1)
            val recorderWorkEntry = RecorderWorkEntry(unidentifiedWork)
            vhs.updateRecord(recorderWorkEntry.id)(ifNotExisting = (recorderWorkEntry, EmptyMetadata()))((_, _) => throw new RuntimeException("Not possible, VHS is empty!"))

            val matcherResult = MatcherResult(Set(MatchedIdentifiers(Set(WorkIdentifier(identifier = recorderWorkEntry.id, version = 1)))))

            val notificationMessage = NotificationMessage(
              MessageId = "MessageId",
              TopicArn = "topic-arn",
              Subject = "subject",
              Message = toJson(matcherResult).get
            )
            sqsClient.sendMessage(queue.url, toJson(notificationMessage).get)

            eventually {
              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)
              val messagesSent = listMessagesReceivedFromSNS(topic)
              messagesSent should have size 1
              assertJsonStringsAreEqual(toJson(unidentifiedWork).get, messagesSent.head.message)
            }
          }
        }
      }
    }
  }

  it("fails if the message sent is not a matcher result") {
    withMergerWorkerServiceFixtures { case (QueuePair(queue, dlq),_, _) =>
          val testObject = TestObject("lallabalula")
      val notificationMessage = NotificationMessage(
        MessageId = "MessageId",
        TopicArn = "topic-arn",
        Subject = "subject",
        Message = toJson(testObject).get
      )
      sqsClient.sendMessage(queue.url, toJson(notificationMessage).get)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
        }
  }

  def withMergerWorkerServiceFixtures[R](testWith: TestWith[(QueuePair, Topic, MetricsSender), R]) = {
    withActorSystem { actorSystem =>
      withLocalSqsQueueAndDlq { case queuePair@QueuePair(queue, dlq) =>
        withLocalSnsTopic { topic =>
          withMockMetricSender { metricsSender =>
            withSQSStream[NotificationMessage, R](actorSystem, queue, metricsSender) { sqsStream =>
              withMergerWorkerService(actorSystem, sqsStream) { _ =>
                testWith((queuePair, topic, metricsSender))
              }
            }
          }
        }
      }
    }
  }

  def withMergerWorkerService[R](actorSystem:ActorSystem, sqsStream: SQSStream[NotificationMessage])(testWith: TestWith[MergerWorkerService, R]) = {
    testWith(new MergerWorkerService(actorSystem, sqsStream))
  }
}
