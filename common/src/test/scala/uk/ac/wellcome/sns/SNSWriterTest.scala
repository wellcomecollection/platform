package uk.ac.wellcome.sns

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.models.aws.S3Config

class SNSWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with SNS
    with S3
    with IntegrationPatience {

  object MessagePointerKeyPrefixGenerator extends KeyPrefixGenerator[String] {
    def generate(obj: String): String = "dummy"
  }

  it(
    "should send a message with subject to the SNS client and return a publish attempt with the id of the request") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val s3Config = S3Config(bucketName = bucket.name)
        val s3 = new S3ObjectStore(s3Client, s3Config, MessagePointerKeyPrefixGenerator)
        val snsConfig = SNSConfig(topic.arn)
        val snsWriter = new SNSWriter(snsClient, snsConfig, s3)
        val message = "sns-writer-test-message"
        val subject = "sns-writer-test-subject"

        val futurePublishAttempt = snsWriter.writeMessage(
          message = message,
          subject = subject
        )

        whenReady(futurePublishAttempt) { publishAttempt =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size (1)
          messages.head.message shouldBe bucket.name
          messages.head.subject shouldBe subject
          publishAttempt.id should be(Right(messages.head.messageId))

          getContentFromS3(bucket, "dummy") should be (message)
        }
      }
    }
  }

  // TODO add test for both invalid bucket and invalid topic
  it("should return a failed future if it fails to publish the message") {
    val s3Config = S3Config(bucketName = "invalid-bucket")
    val s3 = new S3ObjectStore(s3Client, s3Config, MessagePointerKeyPrefixGenerator)
    val snsWriter = new SNSWriter(snsClient, SNSConfig("not a valid topic"), s3)

    val futurePublishAttempt =
      snsWriter.writeMessage(message = "someMessage", subject = "subject")

    whenReady(futurePublishAttempt.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }
}
