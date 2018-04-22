package uk.ac.wellcome.message

import org.mockito.Matchers.{any, anyDouble, anyString, contains, matches}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FunSpec
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.s3.S3Uri
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.sns.NotificationMessage

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class MessageWorkerTest
    extends FunSpec
    with MockitoSugar
    with Eventually
    with ExtendedPatience
    with Metrics
    with Messaging
    with Akka
    with SQS
    with S3 {

  def withFixtures[R] =
    withActorSystem[R] and
      withLocalSqsQueue[R] and
      withMetricsSender[R] _ and
      withLocalS3Bucket[R] and
      withMessageWorker[R](
        sqsClient, s3Client
      ) _

  it("processes messages") {
    withFixtures {
      case (_, queue, metrics, bucket, worker) =>
        val key = "message-key"

        val exampleObject = ExampleObject("some value")
        val json = toJson(exampleObject).get

        s3Client.putObject(bucket.name, key, json)

        val examplePointer = MessagePointer(S3Uri(bucket.name, key))

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = toJson(examplePointer).get,
          Timestamp = "Timestamp",
          SignatureVersion = "SignatureVersion",
          Signature = "Signature",
          SigningCertURL = "SigningCertURL",
          UnsubscribeURL = "UnsubscribeURL"
        )

        sqsClient.sendMessage(
          queue.url,
          toJson(exampleNotification).get
        )

        eventually {
          verify(
            metrics,
            times(1)
          ).timeAndCount(
            matches(".*_ProcessMessage"),
            any()
          )
        }
    }
  }

  it("reports an error when a runtime error occurs") {
    withFixtures {
      case (_, queue, metrics, bucket, worker) =>
        when(
          metrics.timeAndCount[Unit](
            anyString(),
            any[() => Future[Unit]].apply
          )
        ).thenThrow(new RuntimeException)

        val key = "message-key"

        val exampleObject = ExampleObject("some value")
        val json = toJson(exampleObject).get

        s3Client.putObject(bucket.name, key, json)

        val examplePointer = MessagePointer(S3Uri(bucket.name, key))

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = toJson(examplePointer).get,
          Timestamp = "Timestamp",
          SignatureVersion = "SignatureVersion",
          Signature = "Signature",
          SigningCertURL = "SigningCertURL",
          UnsubscribeURL = "UnsubscribeURL"
        )

        sqsClient.sendMessage(
          queue.url,
          toJson(exampleNotification).get
        )

        eventually {
          verify(metrics)
            .incrementCount(
              matches(".*_MessageProcessingFailure"),
              anyDouble())
        }
    }
  }

  it("reports an error when handling unsupported scheme") {
    withFixtures {
      case (_, queue, metrics, bucket, worker) =>
        when(
          metrics.timeAndCount[Unit](
            anyString(),
            any[() => Future[Unit]].apply
          )
        ).thenThrow(new RuntimeException)

        val key = "message-key"

        val exampleObject = ExampleObject("some value")
        val json = toJson(exampleObject).get

        s3Client.putObject(bucket.name, key, json)

        val examplePointer = MessagePointer("http://www.example.com")

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = toJson(examplePointer).get,
          Timestamp = "Timestamp",
          SignatureVersion = "SignatureVersion",
          Signature = "Signature",
          SigningCertURL = "SigningCertURL",
          UnsubscribeURL = "UnsubscribeURL"
        )

        sqsClient.sendMessage(
          queue.url,
          toJson(exampleNotification).get
        )

        eventually {
          verify(metrics)
            .incrementCount(
              matches(".*_MessageProcessingFailure"),
              anyDouble())
        }
    }
  }

  it("reports an error when unable to parse a message") {
    withFixtures {
      case (_, queue, metrics, bucket, worker) =>
        sqsClient.sendMessage(queue.url, "this is not valid Json")

        eventually {
          verify(metrics, never())
            .incrementCount(
              matches(".*_MessageProcessingFailure"),
              anyDouble())
        }
    }
  }
}
