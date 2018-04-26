package uk.ac.wellcome.message

import org.mockito.Matchers.{any, anyDouble, anyString, contains, matches}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.s3.S3ObjectLocation
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.sns.NotificationMessage

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class MessageWorkerTest
    extends FunSpec
    with MockitoSugar
    with Eventually
    with Matchers
    with ExtendedPatience
    with Messaging {

  it("processes messages") {
    withMessageWorkerFixtures {
      case (_, metrics, queue, bucket, worker) =>
        val key = "message-key"

        val exampleObject = ExampleObject("some value")
        val exampleObjectJson = toJson(exampleObject).get

        val examplePointer = MessagePointer(S3ObjectLocation(bucket.name, key))

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = toJson(examplePointer).get
        )

        s3Client.putObject(bucket.name, key, exampleObjectJson)

        sqsClient.sendMessage(
          queue.url,
          toJson(exampleNotification).get
        )

        eventually {
          worker.calledWith shouldBe Some(exampleObject)
        }
    }
  }

  it("increments *_ProcessMessage metric when successful") {
    withMessageWorkerFixturesAndMockedMetrics {
      case (_, metrics, queue, bucket, worker) =>
        val key = "message-key"

        val exampleObject = ExampleObject("some value")
        val exampleObjectJson = toJson(exampleObject).get

        val examplePointer = MessagePointer(S3ObjectLocation(bucket.name, key))

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = toJson(examplePointer).get
        )

        s3Client.putObject(bucket.name, key, exampleObjectJson)

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

  it("increments *_MessageProcessingFailure metric when not successful") {
    withMessageWorkerFixturesAndMockedMetrics {
      case (_, metrics, queue, bucket, worker) =>
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

        val examplePointer = MessagePointer(S3ObjectLocation(bucket.name, key))

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = toJson(examplePointer).get
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

  it("increments *_MessageProcessingFailure metric unable to parse a message") {
    withMessageWorkerFixturesAndMockedMetrics {
      case (_, metrics, queue, bucket, worker) =>
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
