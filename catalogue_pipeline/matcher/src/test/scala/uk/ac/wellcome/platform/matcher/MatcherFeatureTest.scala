package uk.ac.wellcome.platform.matcher

import com.amazonaws.services.s3.AmazonS3
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  WorkIdentifier,
  WorkNode
}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.IdentifierType
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.WorkGraphIdentifiersList
import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}
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

  val sourceIdentifierA = SourceIdentifier(
    identifierType = IdentifierType("sierra-system-number"),
    ontologyType = "Work",
    value = "A")

  it("processes a message with a simple UnidentifiedWork with no linked works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalDynamoDbTable { table =>
            withMatcherServer(queue, storageBucket, topic, table) { _ =>
              val work = UnidentifiedWork(
                sourceIdentifier = sourceIdentifierA,
                identifiers = List(sourceIdentifierA),
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
                    fromJson[WorkGraphIdentifiersList](snsMessage.message).get

                  identifiersList.linkedWorks shouldBe
                    Set(MatchedIdentifiers(
                      Set(WorkIdentifier("sierra-system-number/A", 1))))
                }
              }
            }
          }
        }
      }
    }
  }

  it("does not process a message if the work version is older than that already stored") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalDynamoDbTable { table =>
            withMatcherServer(queue, storageBucket, topic, table) { _ =>

              val existingWorkVersion = 2
              val updatedWorkVersion = 1

              val existingWorkAv2 = WorkNode(
                id = "sierra-system-number/A",
                version = existingWorkVersion,
                linkedIds = Nil,
                componentId = "sierra-system-number/A"
              )
              Scanamo.put(dynamoDbClient)(table.name)(existingWorkAv2)

              val workAv1 = UnidentifiedWork(
                sourceIdentifier = sourceIdentifierA,
                identifiers = List(sourceIdentifierA),
                title = Some("Work"),
                version = updatedWorkVersion
              )

              val workSqsMessage: NotificationMessage =
                hybridRecordNotificationMessage(
                  message = toJson(RecorderWorkEntry(workAv1)).get,
                  version = updatedWorkVersion,
                  s3Client = s3Client,
                  bucket = storageBucket)

              sqsClient.sendMessage(
                queue.url,
                toJson(workSqsMessage).get)

              Thread.sleep(2000)
              eventually {
                noMessagesAreWaitingIn(queue)
                listMessagesReceivedFromSNS(topic).size shouldBe 0
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
