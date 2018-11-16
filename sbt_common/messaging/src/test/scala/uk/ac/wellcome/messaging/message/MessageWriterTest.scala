package uk.ac.wellcome.messaging.message

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest._
import uk.ac.wellcome.messaging.test.fixtures.{MessageInfo, Messaging}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

import scala.concurrent.Future
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

class MessageWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Messaging
    with IntegrationPatience
    with Inside
    with JsonAssertions {

  def createMessage(size: Int) = ExampleObject("a" * size)

  val smallMessage: ExampleObject = createMessage(size = 100)
  val largeMessage: ExampleObject = createMessage(size = 300000)

  val subject = "message-writer-test-subject"

  describe("small messages (<256KB)") {
    it("sends a raw SNS notification") {
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
            val eventualAttempt = messageWriter.write(smallMessage, subject)

            whenReady(eventualAttempt) { pointer =>
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              messages.head.subject shouldBe subject

              val inlineNotification = getInlineNotification(messages.head)
              assertJsonStringsAreEqual(
                inlineNotification.jsonString,
                toJson(smallMessage).get
              )

              listKeysInBucket(bucket) shouldBe List()
            }
          }
        }
      }
    }

    it(
      "sends a raw SNS notification if the message is just under the threshold") {
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
            val message = createMessage(size = 248000)
            val eventualAttempt = messageWriter.write(message, subject)

            whenReady(eventualAttempt) { pointer =>
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              messages.head.subject shouldBe subject

              val inlineNotification = getInlineNotification(messages.head)
              assertJsonStringsAreEqual(
                inlineNotification.jsonString,
                toJson(message).get
              )

              listKeysInBucket(bucket) shouldBe List()
            }
          }
        }
      }
    }

    it("returns a failed future if it fails to publish to SNS") {
      withLocalS3Bucket { bucket =>
        val topic = Topic(arn = "invalid-topic-arn")
        withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
          val eventualAttempt = messageWriter.write(smallMessage, subject)

          whenReady(eventualAttempt.failed) { ex =>
            ex shouldBe a[Throwable]
          }
        }
      }
    }
  }

  describe("large messages (>256KB)") {
    it("sends an S3 pointer") {
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
            val eventualAttempt = messageWriter.write(largeMessage, subject)

            whenReady(eventualAttempt) { pointer =>
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              messages.head.subject shouldBe subject

              val remoteNotification = getRemoteNotification(messages.head)
              val objectLocation = remoteNotification.location

              objectLocation.namespace shouldBe bucket.name

              assertJsonStringsAreEqual(
                getContentFromS3(objectLocation),
                toJson(largeMessage).get
              )
            }
          }
        }
      }
    }

    it("sends an S3 pointer if the message is just larger than the threshold") {
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
            val message = createMessage(size = 250001)
            val eventualAttempt = messageWriter.write(message, subject)

            whenReady(eventualAttempt) { pointer =>
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              messages.head.subject shouldBe subject

              val remoteNotification = getRemoteNotification(messages.head)
              val objectLocation = remoteNotification.location

              objectLocation.namespace shouldBe bucket.name

              assertJsonStringsAreEqual(
                getContentFromS3(objectLocation),
                toJson(message).get
              )
            }
          }
        }
      }
    }

    it("returns a failed future if it fails to store the S3 pointer") {
      withLocalSnsTopic { topic =>
        val bucket = Bucket(name = "invalid-bucket")
        withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
          val eventualAttempt = messageWriter.write(largeMessage, subject)

          whenReady(eventualAttempt.failed) { ex =>
            ex shouldBe a[Throwable]
          }
        }
      }
    }

    it(
      "does not publish a RemoteNotification if it fails to store the S3 pointer") {
      withLocalSnsTopic { topic =>
        val bucket = Bucket(name = "invalid-bucket")
        withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
          val eventualAttempt = messageWriter.write(largeMessage, subject)

          whenReady(eventualAttempt.failed) { _ =>
            listMessagesReceivedFromSNS(topic) should be('empty)
          }
        }
      }
    }

    it("gives distinct S3 keys when sending the same message twice") {
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
            val eventualAttempt1 = messageWriter.write(largeMessage, subject)

            // Wait before sending the next message to increase likelihood they get processed at different timestamps
            Thread.sleep(2)
            val eventualAttempt2 = messageWriter.write(largeMessage, subject)

            whenReady(Future.sequence(List(eventualAttempt1, eventualAttempt2))) {
              _ =>
                val messages = listMessagesReceivedFromSNS(topic)
                messages should have size (2)

                val locations = messages
                  .map { msg =>
                    fromJson[MessageNotification](msg.message).get
                  }
                  .map { _.asInstanceOf[RemoteNotification] }
                  .map { _.location }

                locations.distinct should have size 2
            }
          }
        }
      }
    }
  }

  private def getInlineNotification(info: MessageInfo): InlineNotification = {
    val maybeNotification =
      fromJson[MessageNotification](info.message)

    maybeNotification shouldBe a[Success[_]]
    maybeNotification.get shouldBe a[InlineNotification]

    maybeNotification.get
      .asInstanceOf[InlineNotification]
  }

  private def getRemoteNotification(info: MessageInfo): RemoteNotification = {
    val maybeNotification =
      fromJson[MessageNotification](info.message)

    maybeNotification shouldBe a[Success[_]]
    maybeNotification.get shouldBe a[RemoteNotification]
    maybeNotification.get
      .asInstanceOf[RemoteNotification]
  }
}
