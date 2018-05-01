package uk.ac.wellcome.messaging.message

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectLocation}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Success

class MessageWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Messaging
    with IntegrationPatience
    with Inside {

  val message = ExampleObject("A message sent in the MessageWriterTest")
  val subject = "message-writer-test-subject"

  it("sends messages") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val s3Config = S3Config(bucketName = bucket.name)
        val snsConfig = SNSConfig(topicArn = topic.arn)
        val messageConfig = MessageConfig(
          s3Config = s3Config,
          snsConfig = snsConfig
        )

        val messageWriter = new MessageWriter[ExampleObject](
          messageConfig = messageConfig,
          snsClient = snsClient,
          s3Client = s3Client,
          keyPrefixGenerator = keyPrefixGenerator
        )

        val eventualAttempt = messageWriter.write(message, subject)

        whenReady(eventualAttempt) { pointer =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size (1)
          messages.head.subject shouldBe subject

          val pointer = fromJson[MessagePointer](messages.head.message)

          pointer shouldBe a[Success[_]]
          val messagePointer = pointer.get

          inside(messagePointer) {
            case MessagePointer(S3ObjectLocation(bucketName, key)) => {
              bucketName shouldBe bucket.name

              getContentFromS3(bucket, key) shouldBe toJson(message).get
            }
          }
        }
      }
    }
  }

  it("returns a failed future if it fails to publish to SNS") {
    withLocalS3Bucket { bucket =>
      val s3Config = S3Config(bucketName = bucket.name)
      val snsConfig = SNSConfig(topicArn = "invalid-topic")
      val messageConfig = MessageConfig(
        s3Config = s3Config,
        snsConfig = snsConfig
      )

      val messageWriter = new MessageWriter[ExampleObject](
        messageConfig = messageConfig,
        snsClient = snsClient,
        s3Client = s3Client,
        keyPrefixGenerator = keyPrefixGenerator
      )

      val eventualAttempt = messageWriter.write(message, subject)

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a[Throwable]
      }
    }
  }

  it("returns a failed future if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsConfig = SNSConfig(topicArn = topic.arn)
      val messageConfig = MessageConfig(
        s3Config = s3Config,
        snsConfig = snsConfig
      )

      val messageWriter = new MessageWriter[ExampleObject](
        messageConfig = messageConfig,
        snsClient = snsClient,
        s3Client = s3Client,
        keyPrefixGenerator = keyPrefixGenerator
      )

      val eventualAttempt = messageWriter.write(message, subject)

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a[Throwable]
      }
    }
  }

  it("does not publish message pointer if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsConfig = SNSConfig(topicArn = topic.arn)
      val messageConfig = MessageConfig(
        s3Config = s3Config,
        snsConfig = snsConfig
      )

      val messageWriter = new MessageWriter[ExampleObject](
        messageConfig = messageConfig,
        snsClient = snsClient,
        s3Client = s3Client,
        keyPrefixGenerator = keyPrefixGenerator
      )

      val eventualAttempt = messageWriter.write(message, subject)

      whenReady(eventualAttempt.failed) { _ =>
        listMessagesReceivedFromSNS(topic) should be('empty)
      }
    }
  }

}
