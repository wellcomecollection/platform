package uk.ac.wellcome.messaging.message

import org.scalatest.{Assertion, FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.Random
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class MessageSenderTest
  extends FunSpec
    with Matchers
    with Messaging
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with S3
    with SNS
    with MetricsSenderFixture {


  describe("with S3TypeMessageSender") {
    it("sends messages") {
      withLocalS3Bucket { bucket =>
        withLocalSnsTopic { topic =>
          withSNSWriter(snsClient, topic) { snsWriter =>
            withS3TypeStore[ExampleObject, Assertion](s3Client, S3Config(bucketName = bucket.name)) { s3TypeStore =>
              withS3TypeMessageSender[ExampleObject, Assertion](snsWriter, s3TypeStore) { messageSender =>
                val subject = Random.nextString(10)

                val exampleObject = ExampleObject(Random.nextString(15))

                val attempt = messageSender.send(exampleObject, subject)

                whenReady(attempt) { _ =>
                  val messages = listMessagesReceivedFromSNS(topic)

                  messages.length shouldBe 1
                  messages.head.subject shouldBe subject

                  val actualMessage = get[ExampleObject](messages.head)
                  
                  exampleObject shouldBe actualMessage
                }
              }
            }
          }
        }
      }
    }
  }

  describe("with TypeMessageSender") {
    it("sends messages") {
      withLocalSnsTopic { topic =>
        withSNSWriter(snsClient, topic) { snsWriter =>
          withTypeMessageSender[ExampleObject, Assertion](snsWriter) { messageSender =>
            val subject = Random.nextString(10)

            val exampleObject = ExampleObject(Random.nextString(15))

            val attempt = messageSender.send(exampleObject, subject)

            whenReady(attempt) { _ =>
              val messages = listMessagesReceivedFromSNS(topic)

              messages.length shouldBe 1
              messages.head.subject shouldBe subject

              val actualMessage = fromJson[ExampleObject](messages.head.message).get

              exampleObject shouldBe actualMessage
            }
          }
        }
      }
    }
  }
}

