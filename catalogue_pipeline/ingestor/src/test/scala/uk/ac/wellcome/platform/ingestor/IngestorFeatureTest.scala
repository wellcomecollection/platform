package uk.ac.wellcome.platform.ingestor

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.generators.WorksGenerators

import scala.collection.JavaConverters._

class IngestorFeatureTest
    extends FunSpec
    with Matchers
    with JsonAssertions
    with ScalaFutures
    with fixtures.Server
    with ElasticsearchFixtures
    with Messaging
    with SQS
    with WorksGenerators {

  val itemType = "work"

  it(
    "reads a miro identified work from the queue and ingests it in the v1 and v2 index") {
    val work = createIdentifiedWork

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        sendMessage[IdentifiedBaseWork](
          bucket = bucket,
          queue = queue,
          obj = work)
        withLocalElasticsearchIndex(itemType = itemType) { indexName =>
          withServer(queue, bucket, indexName, itemType) { _ =>
            assertElasticsearchEventuallyHasWork(indexName, itemType, work)
          }
        }
      }
    }
  }

  it(
    "reads a sierra identified work from the queue and ingests it in the v2 index only") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = createSierraSystemSourceIdentifierType
      )
    )

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        sendMessage[IdentifiedBaseWork](
          bucket = bucket,
          queue = queue,
          obj = work)
        withLocalElasticsearchIndex(itemType = itemType) { indexName =>
          withServer(queue, bucket, indexName, itemType) { _ =>
            assertElasticsearchNeverHasWork(indexName, itemType, work)
          }
        }
      }
    }
  }

  it("does not delete a message from the queue if it fails processing") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalElasticsearchIndex(itemType = itemType) { indexName =>
          withServer(queue, bucket, indexName, itemType) { _ =>
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
}
