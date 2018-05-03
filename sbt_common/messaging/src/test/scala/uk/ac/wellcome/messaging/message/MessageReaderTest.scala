package uk.ac.wellcome.messaging.message

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest._
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future


class MessageReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Messaging
    with S3 with ExtendedPatience {


  it("reads and deletes messages") {
    withExampleObjectMessageReaderFixtures {
      case (bucket, messageReader, queue) =>

        val key = "message-key"
        val exampleObject = ExampleObject("some value")

        val notice = put(exampleObject, S3ObjectLocation(bucket.name, key))

        sqsClient.sendMessage(
          queue.url,
          notice
        )

        var received: List[ExampleObject] = Nil
        val f = (o: ExampleObject) => {

          synchronized {
            received = o :: received
          }

          Future.successful(())
        }

        val future = messageReader.readAndDelete(f)

        whenReady(future) { _ =>
          received shouldBe List(exampleObject)

          assertQueueEmpty(queue)
        }
    }
  }

  it("fail gracefully when NotificationMessage cannot be deserialised") {
    withExampleObjectMessageReaderFixtures {
      case (bucket, messageReader, queue) =>
        sqsClient.sendMessage(
          queue.url,
          "not valid json"
        )

        var received: List[ExampleObject] = Nil
        val f = (o: ExampleObject) => {

          synchronized {
            received = o :: received
          }

          Future.successful(())
        }

        val future = messageReader.readAndDelete(f)

        whenReady(future) { _ =>
          received shouldBe Nil

          assertQueueNotEmpty(queue)
        }
    }
  }

  it("does not fail gracefully when the s3 object cannot be retrieved") {
    withExampleObjectMessageReaderFixtures {
      case (bucket, messageReader, queue) =>
        val key = "key.json"

        // Do NOT put S3 object here

        val examplePointer =
          MessagePointer(S3ObjectLocation(bucket.name, key))
        val serialisedExamplePointer = toJson(examplePointer).get

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = serialisedExamplePointer
        )

        val serialisedExampleNotification = toJson(exampleNotification).get

        sqsClient.sendMessage(
          queue.url,
          serialisedExampleNotification
        )

        var received: List[ExampleObject] = Nil
        val f = (o: ExampleObject) => {

          synchronized {
            received = o :: received
          }

          Future.successful(())
        }

        val future = messageReader.readAndDelete(f)

        whenReady(future.failed) { throwable =>
          throwable shouldBe a[AmazonS3Exception]

          received shouldBe Nil

          assertQueueNotEmpty(queue)
        }
    }
  }
}
