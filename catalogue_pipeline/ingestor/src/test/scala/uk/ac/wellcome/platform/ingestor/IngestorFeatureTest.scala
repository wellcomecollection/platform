package uk.ac.wellcome.platform.ingestor

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.sqs.SQSMessage
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConverters._

class IngestorFeatureTest
    extends FunSpec
    with Matchers
    with JsonTestUtil
    with ScalaFutures
    with fixtures.Server
    with ElasticsearchFixtures
    with Messaging
    with SQS {

  val itemType = "work"

  it(
    "reads a miro identified work from the queue and ingests it in the v1 and v2 index") {
    val sourceIdentifier =
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "Item", "5678")

    val work = IdentifiedWork(
      title = Some("A type of a tame turtle"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = "1234")

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        sendToSqs(work, queue, bucket)
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            val flags = messageReaderLocalFlags(bucket, queue) ++ esLocalFlags(
              indexNameV1,
              indexNameV2,
              itemType)
            withServer(flags) { _ =>
              assertElasticsearchEventuallyHasWork(work, indexNameV1, itemType)
              assertElasticsearchEventuallyHasWork(work, indexNameV2, itemType)
            }
          }
        }
      }
    }
  }

  it(
    "reads a sierra identified work from the queue and ingests it in the v2 index only") {
    val sourceIdentifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Item", "5678")

    val work = IdentifiedWork(
      title = Some("A type of a tame turtle"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = "1234")

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        sendToSqs(work, queue, bucket)
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            val flags = messageReaderLocalFlags(bucket, queue) ++ esLocalFlags(
              indexNameV1,
              indexNameV2,
              itemType)
            withServer(flags) { _ =>
              assertElasticsearchEventuallyHasWork(work, indexNameV2, itemType)
              assertElasticsearchNeverHasWork(work, indexNameV1, itemType)
            }
          }
        }
      }
    }
  }

  it("does not delete a message from the queue if it fails processing") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            val flags = messageReaderLocalFlags(bucket, queue) ++ esLocalFlags(
              indexNameV1,
              indexNameV2,
              itemType) ++ s3LocalFlags(bucket)

            withServer(flags) { _ =>
              val invalidMessage = toJson(
                SQSMessage(
                  Some("identified-item"),
                  "not a json string - this will fail parsing",
                  "ingester",
                  "messageType",
                  "timestamp"
                )
              ).get

              sqsClient.sendMessage(
                queue.url,
                invalidMessage
              )
              // After a message is read, it stays invisible for 1 second and then it gets sent again.
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

  private def sendToSqs(work: IdentifiedWork, queue: Queue, bucket: Bucket) = {
    val messageBody = put[IdentifiedWork](
      obj = work,
      location = S3ObjectLocation(
        bucket = bucket.name,
        key = s"${work.canonicalId}.json"
      )
    )
    sqsClient.sendMessage(queue.url, messageBody)
  }
}
