package uk.ac.wellcome.platform.merger

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.merger.fixtures.{
  LocalWorksVhs,
  MatcherResultFixture
}
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.ExecutionContext.Implicits.global

class MergerFeatureTest
    extends FunSpec
    with Messaging
    with fixtures.Server
    with IntegrationPatience
    with LocalVersionedHybridStore
    with ScalaFutures
    with LocalWorksVhs
    with MatcherResultFixture
    with WorksGenerators {

  it("reads matcher result messages off a queue and deletes them") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalDynamoDbTable { table =>
            withTypeVHS[TransformedBaseWork, EmptyMetadata, Assertion](
              storageBucket,
              table) { vhs =>
              withLocalSqsQueueAndDlq {
                case QueuePair(queue, dlq) =>
                  withServer(
                    queue = queue,
                    topic = topic,
                    storageBucket = storageBucket,
                    messageBucket = messagesBucket,
                    table = table) { _ =>
                    val work = createUnidentifiedWork

                    givenStoredInVhs(vhs, work)

                    val matcherResult = matcherResultWith(Set(Set(work)))
                    sendNotificationToSQS(queue, matcherResult)

                    eventually {
                      assertQueueEmpty(queue)
                      assertQueueEmpty(dlq)
                      val worksSent = getMessages[TransformedBaseWork](topic)
                      worksSent should contain only work
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
