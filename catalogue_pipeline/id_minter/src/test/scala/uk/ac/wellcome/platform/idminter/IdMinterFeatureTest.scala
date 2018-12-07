package uk.ac.wellcome.platform.idminter

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.idminter.fixtures.WorkerServiceFixture

import scala.collection.JavaConverters._

class IdMinterFeatureTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase
    with IntegrationPatience
    with Eventually
    with Matchers
    with WorkerServiceFixture
    with WorksGenerators {

  it("mints the same IDs where source identifiers match") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { identifiersTableConfig =>
            withWorkerService(bucket, topic, queue, identifiersTableConfig) {
              _ =>
                eventuallyTableExists(identifiersTableConfig)
                val work = createUnidentifiedWork

                val messageCount = 5

                (1 to messageCount).foreach { _ =>
                  sendMessage(queue = queue, obj = work)
                }

                eventually {
                  val works = getMessages[IdentifiedBaseWork](topic)
                  works.length shouldBe >=(messageCount)

                  works.map(_.canonicalId).distinct should have size 1
                  works.foreach { receivedWork =>
                    receivedWork
                      .asInstanceOf[IdentifiedWork]
                      .sourceIdentifier shouldBe work.sourceIdentifier
                    receivedWork
                      .asInstanceOf[IdentifiedWork]
                      .title shouldBe work.title
                  }
                }
            }
          }
        }
      }
    }
  }

  it("mints an identifier for a UnidentifiedInvisibleWork") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { identifiersTableConfig =>
            withWorkerService(bucket, topic, queue, identifiersTableConfig) {
              _ =>
                eventuallyTableExists(identifiersTableConfig)
                val work = createUnidentifiedInvisibleWork

                sendMessage(queue = queue, obj = work)

                eventually {
                  val works = getMessages[IdentifiedBaseWork](topic)
                  works.length shouldBe >=(1)

                  val receivedWork = works.head
                  val invisibleWork =
                    receivedWork.asInstanceOf[IdentifiedInvisibleWork]
                  invisibleWork.sourceIdentifier shouldBe work.sourceIdentifier
                  invisibleWork.canonicalId shouldNot be(empty)
                }
            }
          }
        }
      }
    }
  }

  it("mints an identifier for a UnidentifiedRedirectedWork") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { identifiersTableConfig =>
            withWorkerService(bucket, topic, queue, identifiersTableConfig) {
              _ =>
                eventuallyTableExists(identifiersTableConfig)

                val work = createUnidentifiedRedirectedWork

                sendMessage(queue = queue, obj = work)

                eventually {
                  val works = getMessages[IdentifiedBaseWork](topic)
                  works.length shouldBe >=(1)

                  val receivedWork = works.head
                  val redirectedWork =
                    receivedWork.asInstanceOf[IdentifiedRedirectedWork]
                  redirectedWork.sourceIdentifier shouldBe work.sourceIdentifier
                  redirectedWork.canonicalId shouldNot be(empty)
                  redirectedWork.redirect.canonicalId shouldNot be(empty)
                }
            }
          }
        }
      }
    }
  }

  it("continues if something fails processing a message") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withIdentifiersDatabase { identifiersTableConfig =>
          withLocalS3Bucket { bucket =>
            withWorkerService(bucket, topic, queue, identifiersTableConfig) {
              _ =>
                sendInvalidJSONto(queue)

                val work = createUnidentifiedWork

                sendMessage(queue = queue, obj = work)

                eventually {
                  val snsMessages = listMessagesReceivedFromSNS(topic)
                  snsMessages.size should be >= 1

                  assertMessageIsNotDeleted(queue)
                }
            }
          }
        }
      }
    }
  }

  private def assertMessageIsNotDeleted(queue: Queue): Unit = {
    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the id_minter picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    sqsClient
      .getQueueAttributes(
        queue.url,
        List("ApproximateNumberOfMessagesNotVisible").asJava
      )
      .getAttributes
      .get("ApproximateNumberOfMessagesNotVisible") shouldBe "1"
  }
}
