package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class MergerWorkerServiceTest extends FunSpec with Messaging with Akka with ExtendedPatience{
  case class TestObject(something: String)

  it("reads and deletes matcher result messages off a queue") {
    withActorSystem
      withMessageStreamFixtures[MatcherResult, Assertion] { case (actorSystem, bucket, messageStream, QueuePair(queue, dlq), _) =>
        withMergerWorkerService (actorSystem, messageStream){_ =>
          val matcherResult = MatcherResult(Set(MatchedIdentifiers(Set(WorkIdentifier(identifier = "sierra/b123456", version = 1)))))

          val messageToSend = put(matcherResult, ObjectLocation(bucket.name, "matcher-result.json"))
          sqsClient.sendMessage(queue.url, messageToSend)

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
        }
      }
  }

  it("fails if the message sent is not a matcher result") {
    withActorSystem
      withMessageStreamFixtures[MatcherResult, Assertion] { case (actorSystem, bucket, messageStream, QueuePair(queue, dlq), _) =>
        withMergerWorkerService (actorSystem, messageStream){_ =>
          val testObject = TestObject("lallabalula")

          val messageToSend = put(testObject, ObjectLocation(bucket.name, "test-object.json"))
          sqsClient.sendMessage(queue.url, messageToSend)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
        }
      }
  }

  def withMergerWorkerService[R](actorSystem:ActorSystem, messageStream: MessageStream[MatcherResult])(testWith: TestWith[MergerWorkerService, R]) = {
    testWith(new MergerWorkerService(actorSystem, messageStream))
  }
}
