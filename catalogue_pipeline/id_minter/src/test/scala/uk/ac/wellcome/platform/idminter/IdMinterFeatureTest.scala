package uk.ac.wellcome.platform.idminter

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.message.MessagePointer
import uk.ac.wellcome.messaging.sqs.SQSMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{MessageInfo, Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConversions._

class IdMinterFeatureTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase
    with fixtures.Server
    with ExtendedPatience
    with Eventually
    with Matchers {

  it("mints the same IDs where source identifiers match") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { dbConfig =>
            val flags =
              sqsLocalFlags(queue) ++
                snsLocalFlags(topic) ++
                dbConfig.flags ++
                messagingLocalFlags(bucket, topic)

            withServer(flags) { _ =>
              eventuallyTableExists(dbConfig)

              val miroID = "M0001234"
              val title = "A limerick about a lion"

              val identifier =
                SourceIdentifier(
                  IdentifierSchemes.miroImageNumber,
                  "Work",
                  miroID)

              val work = UnidentifiedWork(
                title = Some(title),
                sourceIdentifier = identifier,
                version = 1,
                identifiers = List(identifier))

              val messageCount = 5

              (1 to messageCount).foreach { i =>
                val messageBody = put[UnidentifiedWork](
                  obj = work,
                  location = S3ObjectLocation(
                    bucket = bucket.name,
                    key = s"$i.json"
                  )
                )

                sqsClient.sendMessage(queue.url, messageBody)
              }

              def getWorksFromMessages(messages: List[MessageInfo]) =
                messages.map(m => fromJson[IdentifiedWork](m.message).get)

              eventually {
                val messages = listMessagesReceivedFromSNS(topic)
                messages.length shouldBe >=(messageCount)

                val works = getWorksFromMessages(messages)
                works.map(_.canonicalId).distinct should have size 1
                works.foreach { work =>
                  work.identifiers.head.value shouldBe miroID
                  work.title shouldBe Some(title)
                }
              }
            }
          }
        }
      }
    }
  }

  it("continues if something fails processing a message") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withIdentifiersDatabase { dbConfig =>
          withLocalS3Bucket { bucket =>
            val flags =
              sqsLocalFlags(queue) ++
                snsLocalFlags(topic) ++
                dbConfig.flags ++
                messagingLocalFlags(bucket, topic)

            withServer(flags) { _ =>
              sqsClient.sendMessage(queue.url, "not a json string")

              val miroId = "1234"

              val identifier =
                SourceIdentifier(
                  IdentifierSchemes.miroImageNumber,
                  "Work",
                  miroId)

              val work = UnidentifiedWork(
                title = Some("A query about a queue of quails"),
                sourceIdentifier = identifier,
                version = 1,
                identifiers = List(identifier))

              val messageBody = put[UnidentifiedWork](
                obj = work,
                location = S3ObjectLocation(
                  bucket = bucket.name,
                  key = s"key.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size should be >= 1

                assertMessageIsNotDeleted(queue)
              }
            }
          }
        }
      }
    }
  }

  private def getWorksFromMessages(messages: List[MessageInfo]): List[IdentifiedWork] =
    messages.map{ m =>
      val messagePointer = fromJson[MessagePointer](m.message).get
      val messageContent = getContentFromS3(Bucket(messagePointer.src.bucket), messagePointer.src.key)
      fromJson[IdentifiedWork](messageContent).get
    }

  private def generateSqsMessage(MiroID: String): SQSMessage = {
    val identifier =
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", MiroID)

    val work = UnidentifiedWork(
      title = Some("A query about a queue of quails"),
      sourceIdentifier = identifier,
      version = 1,
      identifiers = List(identifier))

    SQSMessage(
      Some("subject"),
      JsonUtil.toJson(work).get,
      "topic",
      "messageType",
      "timestamp")
  }

  private def assertMessageIsNotDeleted(queue: Queue): Unit = {
    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the id_minter picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    eventually {
      sqsClient
        .getQueueAttributes(
          queue.url,
          List("ApproximateNumberOfMessagesNotVisible")
        )
        .getAttributes
        .get("ApproximateNumberOfMessagesNotVisible") shouldBe "1"
    }
  }
}
