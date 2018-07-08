package uk.ac.wellcome.platform.idminter

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConverters._

class IdMinterFeatureTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase
    with fixtures.Server
    with ExtendedPatience
    with Eventually
    with Matchers
    with WorksUtil {

  it("mints the same IDs where source identifiers match") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { identifiersTableConfig =>
            val flags =
              identifiersLocalDbFlags(identifiersTableConfig) ++
                messagingLocalFlags(bucket, topic, queue)

            withServer(flags) { _ =>
              eventuallyTableExists(identifiersTableConfig)
              val work = createUnidentifiedWork

              val messageCount = 5

              (1 to messageCount).foreach { i =>
                val messageBody = put[UnidentifiedWork](
                  obj = work,
                  location = ObjectLocation(
                    namespace = bucket.name,
                    key = s"$i.json"
                  )
                )
                sqsClient.sendMessage(queue.url, messageBody)
              }

              eventually {
                val messages = listMessagesReceivedFromSNS(topic)
                messages.length shouldBe >=(messageCount)

                val works =
                  messages.map(message => get[IdentifiedBaseWork](message))
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
            val flags =
              identifiersLocalDbFlags(identifiersTableConfig) ++
                messagingLocalFlags(bucket, topic, queue)

            withServer(flags) { _ =>
              eventuallyTableExists(identifiersTableConfig)
              val work = createUnidentifiedInvisibleWork

              val messageBody = put[UnidentifiedInvisibleWork](
                obj = work,
                location = ObjectLocation(
                  namespace = bucket.name,
                  key = "invisible-work.json"
                )
              )
              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                val messages = listMessagesReceivedFromSNS(topic)
                messages.length shouldBe >=(1)

                val receivedWork = get[IdentifiedBaseWork](messages.head)
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
            val flags =
              identifiersLocalDbFlags(identifiersTableConfig) ++
                messagingLocalFlags(bucket, topic, queue)

            withServer(flags) { _ =>
              eventuallyTableExists(identifiersTableConfig)

              val work = createUnidentifiedRedirectedWork

              val messageBody = put[UnidentifiedRedirectedWork](
                obj = work,
                location = ObjectLocation(
                  namespace = bucket.name,
                  key = "redirected-work.json"
                )
              )
              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                val messages = listMessagesReceivedFromSNS(topic)
                messages.length shouldBe >=(1)

                val receivedWork = get[IdentifiedBaseWork](messages.head)
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
            val flags =
              identifiersLocalDbFlags(identifiersTableConfig) ++
                messagingLocalFlags(bucket, topic, queue)

            withServer(flags) { _ =>
              sqsClient.sendMessage(
                queue.url,
                "Not a valid JSON string or UnidentifiedWork")

              val work = createUnidentifiedWork

              val messageBody = put[UnidentifiedWork](
                obj = work,
                location = ObjectLocation(
                  namespace = bucket.name,
                  key = s"key.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)

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
