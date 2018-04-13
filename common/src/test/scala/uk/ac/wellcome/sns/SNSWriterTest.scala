package uk.ac.wellcome.sns

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.models.aws.S3Config
import org.scalatest.EitherValues

class SNSWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with SNS
    with S3
    with IntegrationPatience
    with EitherValues {

  it(
    "should send a message with subject to the SNS client and return a publish attempt with the id of the request") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val s3Config = S3Config(bucketName = bucket.name)
        val snsConfig = SNSConfig(topic.arn)
        val snsWriter = new SNSWriter(snsClient, snsConfig, s3Client, s3Config)
        val message = "sns-writer-test-message"
        val subject = "sns-writer-test-subject"

        val eventualAttempt = snsWriter.writeMessage(message, subject)

        whenReady(eventualAttempt) { publishAttempt =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size (1)
          messages.head.message shouldBe bucket.name
          messages.head.subject shouldBe subject
          publishAttempt.right.value.id should be(messages.head.messageId)

          getContentFromS3(bucket, publishAttempt.right.value.key) should be(
            message)
        }
      }
    }
  }

  it(
    "should return a failed future if it fails to publish the message pointer") {
    withLocalS3Bucket { bucket =>
      val s3Config = S3Config(bucketName = bucket.name)
      val snsWriter = new SNSWriter(
        snsClient,
        SNSConfig("not a valid topic"),
        s3Client,
        s3Config)

      val eventualAttempt = snsWriter.writeMessage("someMessage", "subject")

      whenReady(eventualAttempt) { publishAttempt =>
        publishAttempt.isLeft should be(true)
      }
    }
  }

  it("should return a failed future if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsWriter =
        new SNSWriter(snsClient, SNSConfig(topic.arn), s3Client, s3Config)

      val eventualAttempt = snsWriter.writeMessage("someMessage", "subject")

      whenReady(eventualAttempt) { publishAttempt =>
        publishAttempt.isLeft should be(true)
      }
    }
  }

  it("should not publish message pointer if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsWriter =
        new SNSWriter(snsClient, SNSConfig(topic.arn), s3Client, s3Config)

      val eventualAttempt = snsWriter.writeMessage("someMessage", "subject")

      whenReady(eventualAttempt) { publishAttempt =>
        listMessagesReceivedFromSNS(topic) should be('empty)
      }
    }
  }
}
