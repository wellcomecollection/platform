package uk.ac.wellcome.messaging.message

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
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

  val message = ExampleObject("A message sent in the MessageWriterTest")
  val subject = "message-writer-test-subject"

  it("sends messages") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
          val eventualAttempt = messageWriter.write(message, subject)

          whenReady(eventualAttempt) { pointer =>
            val messages = listMessagesReceivedFromSNS(topic)
            messages should have size (1)
            messages.head.subject shouldBe subject

            val maybeNotification =
              fromJson[MessageNotification[ExampleObject]](messages.head.message)

            maybeNotification shouldBe a[Success[_]]
            maybeNotification.get shouldBe a[RemoteNotification[_]]
            val objectLocation = maybeNotification.get
              .asInstanceOf[RemoteNotification[ExampleObject]]
              .location

            objectLocation.namespace shouldBe bucket.name

            assertJsonStringsAreEqual(
              getContentFromS3(
                bucket = Bucket(objectLocation.namespace),
                key = objectLocation.key
              ),
              toJson(message).get
            )
          }
        }
      }
    }
  }

  it("returns a failed future if it fails to publish to SNS") {
    withLocalS3Bucket { bucket =>
      val topic = Topic(arn = "invalid-topic-arn")
      withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
        val eventualAttempt = messageWriter.write(message, subject)

        whenReady(eventualAttempt.failed) { ex =>
          ex shouldBe a[Throwable]
        }
      }
    }
  }

  it("returns a failed future if it fails to store message") {
    withLocalSnsTopic { topic =>
      val bucket = Bucket(name = "invalid-bucket")
      withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
        val eventualAttempt = messageWriter.write(message, subject)

        whenReady(eventualAttempt.failed) { ex =>
          ex shouldBe a[Throwable]
        }
      }
    }
  }

  it("does not publish message pointer if it fails to store message") {
    withLocalSnsTopic { topic =>
      val bucket = Bucket(name = "invalid-bucket")
      withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
        val eventualAttempt = messageWriter.write(message, subject)

        whenReady(eventualAttempt.failed) { _ =>
          listMessagesReceivedFromSNS(topic) should be('empty)
        }
      }
    }
  }

  it("gives distinct s3 keys when sending the same message twice") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        withExampleObjectMessageWriter(bucket, topic) { messageWriter =>
          val eventualAttempt1 = messageWriter.write(message, subject)
          // Wait before sending the next message to increase likelihood they get processed at different timestamps
          Thread.sleep(2)
          val eventualAttempt2 = messageWriter.write(message, subject)

          whenReady(Future.sequence(List(eventualAttempt1, eventualAttempt2))) {
            _ =>
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size (2)

              val locations = messages
                .map { msg => fromJson[MessageNotification[ExampleObject]](msg.message).get }
                .map { _.asInstanceOf[RemoteNotification[ExampleObject]] }
                .map { _.location }

              locations.distinct should have size 2
          }
        }
      }
    }
  }

}
