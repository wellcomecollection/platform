package uk.ac.wellcome.messaging.message

import org.scalatest.{Assertion, FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.s3.{S3Config, S3ObjectLocation}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import uk.ac.wellcome.utils.JsonUtil._

class MessageRetrieverTest
    extends FunSpec
    with Matchers
    with Messaging
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with S3
    with MetricsSenderFixture {

  describe("with S3TypeMessageRetriever") {
    it("retrieves messages") {
      withLocalS3Bucket { bucket =>
        withS3TypeStore[ExampleObject, Assertion](
          s3Client,
          S3Config(bucket.name)) { typeStore =>
          withS3TypeMessageRetriever[ExampleObject, Assertion](typeStore) {
            retriever =>
              val exampleObject = ExampleObject(Random.nextString(15))

              val s3Location = S3ObjectLocation(
                bucket = bucket.name,
                key = Random.nextString(5)
              )

              put(exampleObject, s3Location)

              val messagePointer = MessagePointer(s3Location)

              val notificationMessage = NotificationMessage(
                MessageId = Random.nextString(5),
                TopicArn = Random.nextString(5),
                Subject = Random.nextString(5),
                Message = toJson(messagePointer).get
              )

              val retrieval = retriever.retrieve(notificationMessage)

              whenReady(retrieval) { result =>
                result shouldBe exampleObject
              }
          }
        }
      }
    }
  }

  describe("with TypeMessageRetriever") {
    it("retrieves messages") {
      withTypeMessageRetriever[ExampleObject, Assertion]() { retriever =>
        val exampleObject = ExampleObject(Random.nextString(15))

        val notificationMessage = NotificationMessage(
          MessageId = Random.nextString(5),
          TopicArn = Random.nextString(5),
          Subject = Random.nextString(5),
          Message = toJson(exampleObject).get
        )

        val retrieval = retriever.retrieve(notificationMessage)

        whenReady(retrieval) { result =>
          result shouldBe exampleObject
        }
      }
    }
  }
}
