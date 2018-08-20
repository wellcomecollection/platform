package uk.ac.wellcome.platform.matcher

import com.amazonaws.services.s3.AmazonS3
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier, WorkNode}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessagePointer
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.ObjectLocation

class MatcherFeatureTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with Eventually
    with MatcherFixtures
    with WorksUtil {

  it("processes a message with a simple UnidentifiedWork with no linked works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
            withSpecifiedLocalDynamoDbTable(createWorkGraphTable) {
              graphTable =>
                withMatcherServer(
                  queue,
                  storageBucket,
                  topic,
                  graphTable,
                  lockTable) { _ =>
                  val work = createUnidentifiedWork

                  val workSqsMessage: NotificationMessage =
                    messagePointerNotificationMessage(
                      message = toJson[TransformedBaseWork](work).get,
                      version = 1,
                      s3Client = s3Client,
                      bucket = storageBucket
                    )
                  sendMessage(queue = queue, obj = workSqsMessage)

                  eventually {
                    val snsMessages = listMessagesReceivedFromSNS(topic)
                    snsMessages.size should be >= 1

                    snsMessages.map { snsMessage =>
                      val identifiersList =
                        fromJson[MatcherResult](snsMessage.message).get

                      identifiersList shouldBe
                        MatcherResult(
                          Set(
                            MatchedIdentifiers(
                              Set(WorkIdentifier(
                                identifier = work.sourceIdentifier.toString,
                                version = 1))
                            )))
                    }
                  }
                }
            }
          }
        }
      }
    }
  }

  it(
    "does not process a message if the work version is older than that already stored") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueueAndDlq { queuePair =>
        withLocalS3Bucket { storageBucket =>
          withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
            withSpecifiedLocalDynamoDbTable(createWorkGraphTable) {
              graphTable =>
                withMatcherServer(
                  queuePair.queue,
                  storageBucket,
                  topic,
                  graphTable,
                  lockTable) { _ =>
                  val existingWorkVersion = 2
                  val updatedWorkVersion = 1

                  val workAv1 = createUnidentifiedWorkWith(
                    version = updatedWorkVersion
                  )

                  val existingWorkAv2 = WorkNode(
                    id = workAv1.sourceIdentifier.toString,
                    version = existingWorkVersion,
                    linkedIds = Nil,
                    componentId = workAv1.sourceIdentifier.toString
                  )
                  Scanamo.put(dynamoDbClient)(graphTable.name)(existingWorkAv2)

                  val workSqsMessage: NotificationMessage =
                    messagePointerNotificationMessage(
                      message = toJson[TransformedBaseWork](workAv1).get,
                      version = updatedWorkVersion,
                      s3Client = s3Client,
                      bucket = storageBucket)

                  sendMessage(queue = queuePair.queue, obj = workSqsMessage)

                  eventually {
                    noMessagesAreWaitingIn(queuePair.queue)
                    noMessagesAreWaitingIn(queuePair.dlq)
                    listMessagesReceivedFromSNS(topic).size shouldBe 0
                  }
                }
            }
          }
        }
      }
    }
  }

  def messagePointerNotificationMessage(message: String,
                                        version: Int,
                                        s3Client: AmazonS3,
                                        bucket: Bucket) = {
    val key = "recorder/1/testId/dshg548.json"
    s3Client.putObject(bucket.name, key, message)

    val messagePointer = MessagePointer(ObjectLocation(bucket.name, key))

    createNotificationMessageWith(message = messagePointer)
  }

}
