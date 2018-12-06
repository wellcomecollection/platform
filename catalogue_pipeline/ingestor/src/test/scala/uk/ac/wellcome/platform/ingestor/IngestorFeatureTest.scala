package uk.ac.wellcome.platform.ingestor

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.ingestor.fixtures.WorkerServiceFixture

import scala.collection.JavaConverters._

class IngestorFeatureTest
    extends FunSpec
    with Matchers
    with JsonAssertions
    with ScalaFutures
    with WorkerServiceFixture
    with ElasticsearchFixtures
    with WorksGenerators {

  it("ingests a Miro work") {
    val work = createIdentifiedWork

    withLocalSqsQueue { queue =>
      sendMessage[IdentifiedBaseWork](queue = queue, obj = work)
      withLocalWorksIndex { index =>
        withWorkerService(queue, index) { _ =>
          assertElasticsearchEventuallyHasWork(index, work)
        }
      }
    }
  }

  it("ingests a Sierra work") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    withLocalSqsQueue { queue =>
      sendMessage[IdentifiedBaseWork](queue = queue, obj = work)
      withLocalWorksIndex { index =>
        withWorkerService(queue, index) { _ =>
          assertElasticsearchEventuallyHasWork(index, work)
        }
      }
    }
  }

  it("does not delete a message from the queue if it fails processing") {
    withLocalSqsQueue { queue =>
      withLocalWorksIndex { index =>
        withWorkerService(queue, index) { _ =>
          sendNotificationToSQS(
            queue = queue,
            body = "not a json string -- this will fail parsing"
          )

          // After a message is read, it stays invisible for 1 second and then it gets sent again.		               assertQueueHasSize(queue, size = 1)
          // So we wait for longer than the visibility timeout and then we assert that it has become
          // invisible again, which means that the ingestor picked it up again,
          // and so it wasn't deleted as part of the first run.
          // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
          Thread.sleep(2000)

          eventually {
            sqsClient
              .getQueueAttributes(
                queue.url,
                List("ApproximateNumberOfMessagesNotVisible").asJava
              )
              .getAttributes
              .get(
                "ApproximateNumberOfMessagesNotVisible"
              ) shouldBe "1"
          }
        }
      }
    }
  }
}
