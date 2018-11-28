package uk.ac.wellcome.platform.matcher

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier,
  WorkNode
}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

import scala.concurrent.ExecutionContext.Implicits.global

class MatcherFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with MatcherFixtures
    with WorksGenerators {

  it("processes a message with a simple UnidentifiedWork with no linked works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withWorkerService(queue, topic) { _ =>
          val work = createUnidentifiedWork

          sendMessage[TransformedBaseWork](queue = queue, work)

          eventually {
            val snsMessages = listMessagesReceivedFromSNS(topic)
            snsMessages.size should be >= 1

            snsMessages.map { snsMessage =>
              val identifiersList =
                fromJson[MatcherResult](snsMessage.message).get

              identifiersList shouldBe
                MatcherResult(
                  Set(MatchedIdentifiers(
                    Set(WorkIdentifier(work))
                  )))
            }
          }
        }
      }
    }
  }

  it(
    "does not process a message if the work version is older than that already stored") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueueAndDlq { queuePair =>
        withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
          withWorkerService(queuePair.queue, topic, graphTable) { _ =>
            val existingWorkVersion = 2
            val updatedWorkVersion = 1

            val workAv1 = createUnidentifiedWorkWith(
              version = updatedWorkVersion
            )

            val existingWorkAv2 = WorkNode(
              id = workAv1.sourceIdentifier.toString,
              version = existingWorkVersion,
              linkedIds = Nil,
              componentId = workAv1.sourceIdentifier.toString
            )
            Scanamo.put(dynamoDbClient)(graphTable.name)(existingWorkAv2)

            sendMessage[TransformedBaseWork](queue = queuePair.queue, workAv1)

            eventually {
              noMessagesAreWaitingIn(queuePair.queue)
              noMessagesAreWaitingIn(queuePair.dlq)
              listMessagesReceivedFromSNS(topic).size shouldBe 0
            }
          }
        }
      }
    }
  }
}
