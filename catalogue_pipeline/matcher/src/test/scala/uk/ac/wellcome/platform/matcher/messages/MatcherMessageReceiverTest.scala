package uk.ac.wellcome.platform.matcher.messages

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.matcher.{EquivalentIdentifiers, MatchResult}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class MatcherMessageReceiverTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with MatcherFixtures
    with Eventually {

  it("sends no redirects for a work without identifiers") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          sendSQS(queue, storageBucket, anUnidentifiedSierraWork)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              assertMessageSent(
                topic,
                MatchResult(
                  Set(EquivalentIdentifiers(Set("sierra-system-number/id"))))
              )
            }
          }
        }
      }
    }
  }

  it(
    "work A with one link to B and no existing works returns a single matched work") {
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
                MatchResult(Set(EquivalentIdentifiers(
                  Set("sierra-system-number/A", "sierra-system-number/B"))))
              )
            }
          }
        }
      }
    }
  }

  it("redirects a work with one link and existing redirects") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            val aIdentifier = aSierraSourceIdentifier("A")
            val bIdentifier = aSierraSourceIdentifier("B")
            val cIdentifier = aSierraSourceIdentifier("C")

            val aWork = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier, bIdentifier))

            sendSQS(queue, storageBucket, aWork)

            eventually {

              assertMessageSent(
                topic,
                MatchResult(
                  Set(
                    EquivalentIdentifiers(
                      Set(
                        "sierra-system-number/A",
                        "sierra-system-number/B"
                      ))))
              )

              val bWork = anUnidentifiedSierraWork.copy(
                sourceIdentifier = bIdentifier,
                identifiers = List(bIdentifier, cIdentifier))

              sendSQS(queue, storageBucket, bWork)

              eventually {

                assertMessageSent(
                  topic,
                  MatchResult(
                    Set(
                      EquivalentIdentifiers(
                        Set(
                          "sierra-system-number/A",
                          "sierra-system-number/B",
                          "sierra-system-number/C"
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

            val aWork = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier, bIdentifier))

            sendSQS(queue, storageBucket, aWork)

            eventually {

              assertMessageSent(
                topic,
                MatchResult(
                  Set(
                    EquivalentIdentifiers(
                      Set(
                        "sierra-system-number/A",
                        "sierra-system-number/B"
                      ))))
              )

              val aWorkWithoutLinks = anUnidentifiedSierraWork.copy(
                sourceIdentifier = aIdentifier,
                identifiers = List(aIdentifier))

              sendSQS(queue, storageBucket, aWorkWithoutLinks)

              eventually {

                assertMessageSent(
                  topic,
                  MatchResult(
                    Set(
                      EquivalentIdentifiers(Set(
                        "sierra-system-number/A"
                      )),
                      EquivalentIdentifiers(Set(
                        "sierra-system-number/B"
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

  private def assertMessageSent(topic: Topic, matchResult: MatchResult) = {
    val snsMessages = listMessagesReceivedFromSNS(topic)
    snsMessages.size should be >= 1

    val actualMatchedWorkLists = snsMessages.map { snsMessage =>
      fromJson[MatchResult](snsMessage.message).get
    }
    actualMatchedWorkLists should contain(matchResult)
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
