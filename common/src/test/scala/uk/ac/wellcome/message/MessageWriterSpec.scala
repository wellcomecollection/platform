package uk.ac.wellcome.message

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest._
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.s3.S3Uri
import uk.ac.wellcome.utils.JsonUtil._
import scala.util.Success
import uk.ac.wellcome.sns.SNSWriter

class MessageWriterSpec
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
        val snsWriter = new SNSWriter(snsClient, snsConfig)
        val messages = new MessageWriter(snsWriter, s3Client, s3Config)
        val message = "sns-writer-test-message"
        val subject = "sns-writer-test-subject"

        val eventualAttempt = messages.write(message, subject)

        whenReady(eventualAttempt) { pointer =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size (1)
          messages.head.subject shouldBe subject

          val pointer = fromJson[MessagePointer](messages.head.message)

          inside(pointer) {
            case Success(MessagePointer(S3Uri(bucketName, _))) =>
              bucketName shouldBe bucket.name
          }

          getContentFromS3(bucket) should contain(message)
        }
      }
    }
  }

  it(
    "should return a failed future if it fails to publish the message pointer") {
    withLocalS3Bucket { bucket =>
      val s3Config = S3Config(bucketName = bucket.name)
      val snsConfig = SNSConfig(topicArn = "invalid-topic")
      val snsWriter = new SNSWriter(snsClient, snsConfig)
      val messages = new MessageWriter(snsWriter, s3Client, s3Config)

      val eventualAttempt = messages.write("someMessage", "subject")

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a[Throwable]
      }
    }
  }

  it("should return a failed future if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsConfig = SNSConfig(topic.arn)
      val snsWriter = new SNSWriter(snsClient, snsConfig)
      val messages = new MessageWriter(snsWriter, s3Client, s3Config)

      val eventualAttempt = messages.write("someMessage", "subject")

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a[Throwable]
      }
    }
  }

  it("should not publish message pointer if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsConfig = SNSConfig(topic.arn)
      val snsWriter = new SNSWriter(snsClient, snsConfig)
      val messages = new MessageWriter(snsWriter, s3Client, s3Config)

      val eventualAttempt = messages.write("someMessage", "subject")

      whenReady(eventualAttempt.failed) { _ =>
        listMessagesReceivedFromSNS(topic) should be('empty)
      }
    }
  }

}
