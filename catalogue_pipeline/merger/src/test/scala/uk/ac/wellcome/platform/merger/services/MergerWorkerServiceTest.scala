package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._
import scala.concurrent.ExecutionContext.Implicits.global

class MergerWorkerServiceTest extends FunSpec with SQS with Akka with ExtendedPatience with MetricsSenderFixture{
  case class TestObject(something: String)

  it("reads and deletes matcher result messages off a queue") {

    withMergerWorkerServiceFixtures { case (QueuePair(queue, dlq), _) =>
              val matcherResult = MatcherResult(Set(MatchedIdentifiers(Set(WorkIdentifier(identifier = "sierra/b123456", version = 1)))))

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
              }
            }
  }

  it("fails if the message sent is not a matcher result") {
    withMergerWorkerServiceFixtures { case (QueuePair(queue, dlq), _) =>
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

  def withMergerWorkerServiceFixtures[R](testWith: TestWith[(QueuePair, MetricsSender), R]) = {
    withActorSystem { actorSystem =>
      withLocalSqsQueueAndDlq { case queuePair@QueuePair(queue, dlq) =>
        withMockMetricSender { metricsSender =>
          withSQSStream[NotificationMessage, R](actorSystem, queue, metricsSender) { sqsStream =>
            withMergerWorkerService(actorSystem, sqsStream) { _ =>
              testWith((queuePair, metricsSender))
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
