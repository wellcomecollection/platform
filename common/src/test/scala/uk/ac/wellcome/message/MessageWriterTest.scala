package uk.ac.wellcome.message

import io.circe.Json
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest._
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectLocation, S3ObjectStore}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Success
import uk.ac.wellcome.sns.SNSWriter

class MessageWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Messaging
    with IntegrationPatience
    with Inside {

  it(
    "sends a message and returns a publish attempt with the id of the request") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val s3Config = S3Config(bucketName = bucket.name)

        val snsConfig = SNSConfig(topic.arn)
        val snsWriter = new SNSWriter(snsClient, snsConfig)

        val s3ObjectStore = new S3ObjectStore[ExampleObject](
          s3Client,
          s3Config,
          keyPrefixGenerator)

        val messages =
          new MessageWriter[ExampleObject](snsWriter, s3Config, s3ObjectStore)

        val message = ExampleObject("Some value")
        val subject = "sns-writer-test-subject"

        val eventualAttempt = messages.write(message, subject)

        whenReady(eventualAttempt) { pointer =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size (1)
          messages.head.subject shouldBe subject

          val pointer = fromJson[MessagePointer](messages.head.message)

          inside(pointer) {
            case Success(MessagePointer(S3ObjectLocation(bucketName, _))) =>
              bucketName shouldBe bucket.name
          }

          getContentFromS3(bucket)
            .map(fromJson[ExampleObject])
            .map(_.get) should contain(message)
        }
      }
    }
  }

  it("returns a failed future if it fails to publish the message pointer") {
    withLocalS3Bucket { bucket =>
      val s3Config = S3Config(bucketName = bucket.name)
      val snsConfig = SNSConfig(topicArn = "invalid-topic")
      val snsWriter = new SNSWriter(snsClient, snsConfig)

      val keyPrefixGenerator: KeyPrefixGenerator[ExampleObject] =
        new KeyPrefixGenerator[ExampleObject] {
          override def generate(obj: ExampleObject): String = "/"
        }

      val s3ObjectStore = new S3ObjectStore[ExampleObject](
        s3Client,
        s3Config,
        keyPrefixGenerator)
      val messages =
        new MessageWriter[ExampleObject](snsWriter, s3Config, s3ObjectStore)

      val message = ExampleObject("Some value")

      val eventualAttempt = messages.write(message, "subject")

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a[Throwable]
      }
    }
  }

  it("returns a failed future if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsConfig = SNSConfig(topic.arn)
      val snsWriter = new SNSWriter(snsClient, snsConfig)

      val keyPrefixGenerator: KeyPrefixGenerator[ExampleObject] =
        new KeyPrefixGenerator[ExampleObject] {
          override def generate(obj: ExampleObject): String = "/"
        }

      val s3ObjectStore = new S3ObjectStore[ExampleObject](
        s3Client,
        s3Config,
        keyPrefixGenerator)
      val messages =
        new MessageWriter[ExampleObject](snsWriter, s3Config, s3ObjectStore)

      val message = ExampleObject("Some value")

      val eventualAttempt = messages.write(message, "subject")

      whenReady(eventualAttempt.failed) { ex =>
        ex shouldBe a[Throwable]
      }
    }
  }

  it("does not publish message pointer if it fails to store message") {
    withLocalSnsTopic { topic =>
      val s3Config = S3Config(bucketName = "invalid-bucket")
      val snsConfig = SNSConfig(topic.arn)
      val snsWriter = new SNSWriter(snsClient, snsConfig)

      val keyPrefixGenerator: KeyPrefixGenerator[ExampleObject] =
        new KeyPrefixGenerator[ExampleObject] {
          override def generate(obj: ExampleObject): String = "/"
        }

      val s3ObjectStore = new S3ObjectStore[ExampleObject](
        s3Client,
        s3Config,
        keyPrefixGenerator)
      val messages =
        new MessageWriter[ExampleObject](snsWriter, s3Config, s3ObjectStore)

      val message = ExampleObject("Some value")

      val eventualAttempt = messages.write(message, "subject")

      whenReady(eventualAttempt.failed) { _ =>
        listMessagesReceivedFromSNS(topic) should be('empty)
      }
    }
  }

}
