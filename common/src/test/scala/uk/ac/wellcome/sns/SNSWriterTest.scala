package uk.ac.wellcome.sns

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest._
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.models.aws.S3Config
import org.scalatest.EitherValues
import uk.ac.wellcome.s3.S3Uri
import uk.ac.wellcome.utils.JsonUtil._
import scala.util.Success

class SNSWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with SNS
    with S3
    with IntegrationPatience
    with Inside {

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

        whenReady(eventualAttempt) { pointer =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size (1)
          messages.head.subject shouldBe subject

          val pointer = fromJson[MessagePointer](messages.head.message)

          inside(pointer) {
            case Success(MessagePointer(S3Uri(bucketName, _))) => bucketName shouldBe bucket.name
          }

          getContentFromS3(bucket) should contain (message)
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

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a [Throwable]
      }
    }
  }

  it("should return a failed future if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsWriter =
        new SNSWriter(snsClient, SNSConfig(topic.arn), s3Client, s3Config)

      val eventualAttempt = snsWriter.writeMessage("someMessage", "subject")

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a [Throwable]
      }
    }
  }

  it("should not publish message pointer if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsWriter =
        new SNSWriter(snsClient, SNSConfig(topic.arn), s3Client, s3Config)

      val eventualAttempt = snsWriter.writeMessage("someMessage", "subject")

      whenReady(eventualAttempt.failed) { _ =>
        listMessagesReceivedFromSNS(topic) should be('empty)
      }
    }
  }

}
