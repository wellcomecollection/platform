package uk.ac.wellcome.platform.merger

import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class MergerFeatureTest extends FunSpec with Messaging with fixtures.Server with ExtendedPatience {
  it("reads matcher result messages off a queue and deletes them") {
    withLocalSqsQueueAndDlq { case QueuePair(queue,dlq) =>
          withServer(queue) { _ =>
            val matcherResult = MatcherResult(Set(MatchedIdentifiers(Set(WorkIdentifier(identifier = "sierra/b123456", version = 1)))))

            sqsClient.sendMessage(queue.url, toJson(matcherResult).get)

            eventually {
              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)
            }
          }
        }
      }
}
