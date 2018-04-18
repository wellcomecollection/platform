package uk.ac.wellcome.platform.idminter

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.{IdentifierSchemes, _}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.fixtures.{MessageInfo, SNS, SQS}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.sqs.MessagePointer
import uk.ac.wellcome.s3.S3Uri

import scala.collection.JavaConversions._

class IdMinterFeatureTest
    extends FunSpec
    with SQS
    with SNS
    with S3
    with fixtures.IdentifiersDatabase
    with fixtures.Server
    with ExtendedPatience
    with Eventually
    with Matchers {

  private def getWorksFromMessages(messages: List[MessageInfo]) = {
    val pointers = messages.map(m => fromJson[MessagePointer](m.message).get)
    pointers.map(loadWork)
  }

  def loadWork(pointer: MessagePointer) = {
    pointer.src match {
      case S3Uri(bucket, key) => fromJson[IdentifiedWork](getContentFromS3(S3.Bucket(bucket), key)).get
      case _ => sys.error("URI scheme unsupported by test")
    }
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

  private def assertMessageIsNotDeleted(queue: SQS.Queue): Unit = {
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

  it(
    "mints the same ID for SourcedWorks that have matching source identifiers") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { dbConfig =>
            val flags = sqsLocalFlags(queue) ++ snsLocalFlags(topic) ++ s3LocalFlags(
              bucket) ++ dbConfig.flags

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

              val sqsMessage = SQSMessage(
                Some("subject"),
                toJson(work).get,
                "topic",
                "messageType",
                "timestamp"
              )

              val messageCount = 5
              val baseKey = "message-key"

              (1 to messageCount).foreach { _ =>
                val key = s"$baseKey/$messageCount"
                s3Client.putObject(bucket.name, key, toJson(sqsMessage).get)
                sqsClient.sendMessage(
                  queue.url,
                  s"""{"src":"s3://${bucket.name}/$key"}""")
              }

              eventually {
                val messages = listMessagesReceivedFromSNS(topic)
                messages.length shouldBe >=(messageCount)

                getWorksFromMessages(messages).foreach { work =>
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
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { dbConfig =>
            val flags = sqsLocalFlags(queue) ++ snsLocalFlags(topic) ++ s3LocalFlags(
              bucket) ++ dbConfig.flags

            withServer(flags) { _ =>
              sqsClient.sendMessage(queue.url, "not a json string")

              val miroId = "1234"
              val sqsMessage = generateSqsMessage(miroId)

              val key = "message-key"
              sqsClient.sendMessage(
                queue.url,
                s"""{"src":"s3://${bucket.name}/$key"}""")
              s3Client.putObject(bucket.name, key, toJson(sqsMessage).get)

              eventually {
                val messages = listMessagesReceivedFromSNS(topic)
                messages should have size (1)
              }

              assertMessageIsNotDeleted(queue)
            }
          }
        }
      }
    }
  }
}
