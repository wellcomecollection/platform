package uk.ac.wellcome.platform.matcher

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{
  IdentifierList,
  LinkedWorksIdentifiersList
}
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class MatcherFeatureTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with Eventually
    with MatcherFixtures {

  it("processes a message with a sinple UnidentifiedWork with no linked works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalDynamoDbTable { table =>
            withMatcherServer(queue, storageBucket, topic, table) { _ =>
              val identifier = SourceIdentifier(
                IdentifierSchemes.sierraSystemNumber,
                "Work",
                "id")

              val work = UnidentifiedWork(
                sourceIdentifier = identifier,
                identifiers = List(identifier),
                title = Some("Work"),
                version = 1
              )
              val workSqsMessage: NotificationMessage =
                hybridRecordNotificationMessage(
                  message = toJson(RecorderWorkEntry(work = work)).get,
                  version = 1,
                  s3Client = s3Client,
                  bucket = storageBucket
                )
              sqsClient.sendMessage(
                queue.url,
                toJson(workSqsMessage).get
              )

              eventually {
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size should be >= 1

                snsMessages.map { snsMessage =>
                  val identifiersList =
                    fromJson[LinkedWorksIdentifiersList](snsMessage.message).get
                  identifiersList.linkedWorks shouldBe Set(
                    IdentifierList(Set("sierra-system-number/id")))
                }
              }
            }
          }
        }
      }
    }
  }

  def hybridRecordNotificationMessage(message: String,
                                      version: Int,
                                      s3Client: AmazonS3,
                                      bucket: Bucket) = {
    val key = "recorder/1/testId/dshg548.json"
    s3Client.putObject(bucket.name, key, message)

    val hybridRecord = HybridRecord(
      id = "testId",
      version = version,
      s3key = key
    )

    NotificationMessage(
      "messageId",
      "topicArn",
      "subject",
      toJson(hybridRecord).get
    )
  }

}
