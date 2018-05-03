package uk.ac.wellcome.messaging.message

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest._
import io.circe.Decoder
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.TestWith
import com.amazonaws.services.sqs.model.Message
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.storage.s3.{
  KeyPrefixGenerator,
  S3Config,
  S3ObjectLocation,
  S3ObjectStore
}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._

import scala.util.{Failure, Success, Try}

class MessageReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Messaging
    with S3 {

  describe("reads a NotificationMessage from an sqs.model.Message") {
    it("converts to type T") {
      withExampleObjectMessageReaderFixtures {
        case (bucket, messageReader) =>
          val key = "key.json"
          val expectedObject = ExampleObject("some value")
          val serialisedExampleObject = toJson(expectedObject).get

          s3Client.putObject(bucket.name, key, serialisedExampleObject)

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

          val exampleMessage = new Message()
            .withBody(serialisedExampleNotification)

          val actualObjectFuture = messageReader.read(exampleMessage)

          whenReady(actualObjectFuture) { actualObject =>
            actualObject shouldBe expectedObject
          }
      }
    }

    it("fail gracefully when NotificationMessage cannot be deserialised") {
      withExampleObjectMessageReaderFixtures {
        case (bucket, messageReader) =>
          val key = "key.json"
          val expectedObject = ExampleObject("some value")
          val serialisedExampleObject = toJson(expectedObject).get

          s3Client.putObject(bucket.name, key, serialisedExampleObject)

          val serialisedExamplePointer = "Not even close to valid json."

          val exampleNotification = NotificationMessage(
            MessageId = "MessageId",
            TopicArn = "TopicArn",
            Subject = "Subject",
            Message = serialisedExamplePointer
          )

          val serialisedExampleNotification = toJson(exampleNotification).get

          val exampleMessage = new Message()
            .withBody(serialisedExampleNotification)

          val actualObjectFuture = messageReader.read(exampleMessage)

          whenReady(actualObjectFuture.failed) { throwable =>
            throwable shouldBe a[GracefulFailureException]
          }
      }
    }

    it("does not fail gracefully when the s3 object cannot be retrieved") {
      withExampleObjectMessageReaderFixtures {
        case (bucket, messageReader) =>
          val key = "key.json"

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

          val exampleMessage = new Message()
            .withBody(serialisedExampleNotification)

          val actualObjectFuture = messageReader.read(exampleMessage)

          whenReady(actualObjectFuture.failed) { throwable =>
            throwable shouldBe a[AmazonS3Exception]
          }
      }
    }
  }
}
