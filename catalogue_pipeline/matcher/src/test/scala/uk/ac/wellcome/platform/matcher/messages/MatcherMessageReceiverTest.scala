package uk.ac.wellcome.platform.matcher.messages

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, WorkIdentifier}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.WorkGraphIdentifiersList
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConverters._


class MatcherMessageReceiverTest
  extends FunSpec
    with Matchers
    with ExtendedPatience
    with MatcherFixtures
    with Eventually {

  it("creates a work without identifiers") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          sendSQS(queue, storageBucket, anUnidentifiedSierraWork)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              assertMessageSent(
                topic,
                WorkGraphIdentifiersList(Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/id", 1)))))
              )
            }
          }
        }
      }
    }
  }

  it("work A with one link to B and no existing works returns a single matched work") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          val linkedIdentifier = aSierraSourceIdentifier("B")
          val aIdentifier = aSierraSourceIdentifier("A")
          val work = anUnidentifiedSierraWork.copy(
            sourceIdentifier = aIdentifier,
            identifiers = List(aIdentifier, linkedIdentifier))

          sendSQS(queue, storageBucket, work)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              assertMessageSent(
                topic,
                WorkGraphIdentifiersList(Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 1),
                    WorkIdentifier("sierra-system-number/B", 0)))))
              )
            }
          }
        }
      }
    }
  }

  it("matches a work with one link then matches the combined work to a new work") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            val aIdentifier = aSierraSourceIdentifier("A")
            val bIdentifier = aSierraSourceIdentifier("B")
            val cIdentifier = aSierraSourceIdentifier("C")

            val aWork = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier))

            sendSQS(queue, storageBucket, aWork)

            eventually {
              assertMessageSent(
                topic,

                WorkGraphIdentifiersList(Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 1)
                  ))))
              )

              val bWorkV1 = anUnidentifiedSierraWork.copy(
                sourceIdentifier = bIdentifier,
                identifiers = List(bIdentifier))

              sendSQS(queue, storageBucket, bWorkV1)

              eventually {
                assertMessageSent(
                  topic,
                  WorkGraphIdentifiersList(Set(
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/B", 1)
                    ))))
                )
              }
              val cWorkV1 = anUnidentifiedSierraWork.copy(
                sourceIdentifier = cIdentifier,
                identifiers = List(cIdentifier))

              sendSQS(queue, storageBucket, cWorkV1)

              eventually {
                assertMessageSent(
                  topic,
                  WorkGraphIdentifiersList(Set(
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/C", 1)
                    ))))
                )
              }

              val aWorkV2 = anUnidentifiedSierraWork.copy(
                sourceIdentifier = aIdentifier,
                version = 2,
                identifiers = List(aIdentifier, bIdentifier))

              sendSQS(queue, storageBucket, aWorkV2)

              eventually {

                assertMessageSent(
                  topic,
                  WorkGraphIdentifiersList(Set(
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/A", 2),
                      WorkIdentifier("sierra-system-number/B", 1)
                    ))
                  ))
                )
              }

              val bWorkV2 = anUnidentifiedSierraWork.copy(
                sourceIdentifier = bIdentifier,
                version = 2,
                identifiers = List(bIdentifier, cIdentifier))

              sendSQS(queue, storageBucket, bWorkV2)

              eventually {

                assertMessageSent(
                  topic,
                  WorkGraphIdentifiersList(Set(
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/A", 2),
                      WorkIdentifier("sierra-system-number/B", 2),
                      WorkIdentifier("sierra-system-number/C", 1)
                    ))))
                )
              }
            }
          }
        }
      }
    }
  }

  it("breaks a set of works into individual works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            val aIdentifier = aSierraSourceIdentifier("A")
            val bIdentifier = aSierraSourceIdentifier("B")

            val workAv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 1,
              identifiers = List(aIdentifier))

            sendSQS(queue, storageBucket, workAv1)

            eventually {
              assertMessageSent(
                topic,
                WorkGraphIdentifiersList(
                  Set(
                    MatchedIdentifiers(
                      Set(
                        WorkIdentifier("sierra-system-number/A", 1)
                      ))))
              )
            }

            val bWorkV1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = bIdentifier,
              version = 1,
              identifiers = List(bIdentifier))

            sendSQS(queue, storageBucket, bWorkV1)

            eventually {
              assertMessageSent(
                topic,
                WorkGraphIdentifiersList(Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/B", 1)
                  ))))
              )
            }

            val workAv2LinkedToB = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 2,
              identifiers = List(aIdentifier, bIdentifier))

            sendSQS(queue, storageBucket, workAv2LinkedToB)

            eventually {
              assertMessageSent(
                topic,
                WorkGraphIdentifiersList(Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 2),
                    WorkIdentifier("sierra-system-number/B", 1)
                  ))))
              )

              val workAv3WithoutLinks = anUnidentifiedSierraWork.copy(
                sourceIdentifier = aIdentifier,
                version = 3,
                identifiers = List(aIdentifier))

              sendSQS(queue, storageBucket, workAv3WithoutLinks)

              eventually {
                assertMessageSent(
                  topic,
                  WorkGraphIdentifiersList(Set(
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/A", 3)
                    )),
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/B", 1)
                    ))
                  ))
                )
              }
            }
          }
        }
      }
    }
  }

  it("fails when processing an old message") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>

            val aIdentifier = aSierraSourceIdentifier("A")

            val workAv2 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 2,
              identifiers = List(aIdentifier)
            )
            sendSQS(queue, storageBucket, workAv2)
            eventually {
              assertMessageSent(
                topic,
                WorkGraphIdentifiersList(Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 2)
                  ))))
              )

              val workAv1 = anUnidentifiedSierraWork.copy(
                sourceIdentifier = aIdentifier,
                identifiers = List(aIdentifier),
                version = 1)

              sendSQS(queue, storageBucket, workAv1)
              Thread.sleep(2000)
              eventually {
                sqsClient
                  .getQueueAttributes(
                    queue.url,
                    List("ApproximateNumberOfMessagesNotVisible").asJava
                  )
                  .getAttributes
                  .get(
                    "ApproximateNumberOfMessagesNotVisible"
                  ) shouldBe "1"
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size shouldBe 0
              }
            }
          }
        }
      }
    }
  }

  private def assertMessageSent(
                                 topic: Topic,
                                 identifiersList: WorkGraphIdentifiersList) = {
    val snsMessages = listMessagesReceivedFromSNS(topic)
    snsMessages.size should be >= 1

    val actualMatchedWorkLists = snsMessages.map { snsMessage =>
      fromJson[WorkGraphIdentifiersList](snsMessage.message).get
    }
    actualMatchedWorkLists should contain(identifiersList)
  }

  private def sendSQS(queue: SQS.Queue,
                      storageBucket: Bucket,
                      work: UnidentifiedWork) = {
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
