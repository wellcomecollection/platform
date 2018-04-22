package uk.ac.wellcome.message

import org.scalatest._
import io.circe.Decoder
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.{S3, TestWith}
import com.amazonaws.services.sqs.model.Message
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore, S3Uri}
import uk.ac.wellcome.sns.NotificationMessage
import uk.ac.wellcome.test.fixtures.S3.Bucket
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._

import scala.util.{Failure, Success, Try}

class MessageReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Messaging
    with S3 {

  def withFixtures[R] = withLocalS3Bucket[R] and withMessageReader[R](s3Client) _

  it(
    "reads a NotificationMessage from an sqs.model.Message and converts to type T") {
    withFixtures {
      case (bucket, messageReader) =>
        val key = "key.json"
        val expectedObject = ExampleObject("some value")
        val serialisedExampleObject = toJson(expectedObject).get

        s3Client.putObject(bucket.name, key, serialisedExampleObject)

        val examplePointer = MessagePointer(S3Uri(bucket.name, key))
        val serialisedExamplePointer = toJson(examplePointer).get

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = serialisedExamplePointer,
          Timestamp = "Timestamp",
          SignatureVersion = "SignatureVersion",
          Signature = "Signature",
          SigningCertURL = "SigningCertURL",
          UnsubscribeURL = "UnsubscribeURL"
        )

        val serialisedExampleNotification = toJson(exampleNotification).get

        val exampleMessage = new Message()
          .withBody(serialisedExampleNotification)

        val actualObjectFuture = messageReader.process(exampleMessage)

        whenReady(actualObjectFuture) { actualObject =>
          expectedObject shouldBe actualObject
        }
    }
  }
}
