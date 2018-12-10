package uk.ac.wellcome.platform.merger

import org.scalatest.FunSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.merger.fixtures.{
  LocalWorksVhs,
  MatcherResultFixture,
  WorkerServiceFixture
}
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class MergerFeatureTest
    extends FunSpec
    with Messaging
    with IntegrationPatience
    with LocalVersionedHybridStore
    with ScalaFutures
    with LocalWorksVhs
    with MatcherResultFixture
    with WorkerServiceFixture
    with WorksGenerators {

  it("reads matcher result messages off a queue and deletes them") {
    withLocalSnsTopic { topic =>
      withTransformedBaseWorkVHS { vhs =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withWorkerService(vhs, topic, queue) { _ =>
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
