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

class MatcherMessageReceiverTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with MatcherFixtures
    with Eventually {

  private val aIdentifier = aSierraSourceIdentifier("A")
  private val bIdentifier = aSierraSourceIdentifier("B")
  private val cIdentifier = aSierraSourceIdentifier("C")

  it("creates a work without identifiers") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            // Work Av1 created without any matched works
            val updatedWork = anUnidentifiedSierraWork
            val expectedMatchedWorks =
              WorkGraphIdentifiersList(
                Set(MatchedIdentifiers(
                  Set(WorkIdentifier("sierra-system-number/id", 1)))))

            processAndAssertMatchedWorkIs(
              updatedWork,
              expectedMatchedWorks,
              queue,
              storageBucket,
              topic)
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
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            // Work Av1
            val workAv1 =
              anUnidentifiedSierraWork.copy(
                sourceIdentifier = aIdentifier,
                identifiers = List(aIdentifier, bIdentifier))
            // Work Av1 matched to B (before B exists hence version 0)
            // need to match to works that do not exist to support
            // bi-directionally matched works without deadlocking (A->B, B->A)
            val expectedMatchedWorks = WorkGraphIdentifiersList(
              Set(
                MatchedIdentifiers(Set(
                  WorkIdentifier("sierra-system-number/A", 1),
                  WorkIdentifier("sierra-system-number/B", 0)))))

            processAndAssertMatchedWorkIs(
              workAv1,
              expectedMatchedWorks,
              queue,
              storageBucket,
              topic)
          }
        }
      }
    }
  }

  it(
    "matches a work with one link then matches the combined work to a new work") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            // Work Av1
            val workAv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier))

            val expectedMatchedWorks = WorkGraphIdentifiersList(
              Set(
                MatchedIdentifiers(Set(
                  WorkIdentifier("sierra-system-number/A", 1)
                ))))

            processAndAssertMatchedWorkIs(
              workAv1,
              expectedMatchedWorks,
              queue,
              storageBucket,
              topic)

            // Work Bv1
            val workBv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = bIdentifier,
              identifiers = List(bIdentifier))

            processAndAssertMatchedWorkIs(
              workBv1,
              WorkGraphIdentifiersList(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic)

            // Work Av1 matched to B
            val workAv2 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 2,
              identifiers = List(aIdentifier, bIdentifier))

            processAndAssertMatchedWorkIs(
              workAv2,
              WorkGraphIdentifiersList(
                Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 2),
                    WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic
            )

            // Work Cv1
            val workCv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = cIdentifier,
              identifiers = List(cIdentifier))

            processAndAssertMatchedWorkIs(
              workCv1,
              WorkGraphIdentifiersList(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/C", 1))))),
              queue,
              storageBucket,
              topic)

            // Work Bv2 matched to C
            val workBv2 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = bIdentifier,
              version = 2,
              identifiers = List(bIdentifier, cIdentifier))

            processAndAssertMatchedWorkIs(
              workBv2,
              WorkGraphIdentifiersList(
                Set(
                  MatchedIdentifiers(
                    Set(
                      WorkIdentifier("sierra-system-number/A", 2),
                      WorkIdentifier("sierra-system-number/B", 2),
                      WorkIdentifier("sierra-system-number/C", 1))))),
              queue,
              storageBucket,
              topic
            )
          }
        }
      }
    }
  }

  it("breaks matched works into individual works") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            // Work Av1
            val workAv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 1,
              identifiers = List(aIdentifier))

            processAndAssertMatchedWorkIs(
              workAv1,
              WorkGraphIdentifiersList(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/A", 1))))),
              queue,
              storageBucket,
              topic)

            // Work Bv1
            val workBv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = bIdentifier,
              version = 1,
              identifiers = List(bIdentifier))

            processAndAssertMatchedWorkIs(
              workBv1,
              WorkGraphIdentifiersList(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic)

            // Match Work A to Work B
            val workAv2MatchedToB = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 2,
              identifiers = List(aIdentifier, bIdentifier))

            processAndAssertMatchedWorkIs(
              workAv2MatchedToB,
              WorkGraphIdentifiersList(
                Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 2),
                    WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic
            )

            // A no longer matches B
            val workAv3WithNoMatchingWorks = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 3,
              identifiers = List(aIdentifier))

            processAndAssertMatchedWorkIs(
              workAv3WithNoMatchingWorks,
              WorkGraphIdentifiersList(
                Set(
                  MatchedIdentifiers(
                    Set(WorkIdentifier("sierra-system-number/A", 3))),
                  MatchedIdentifiers(
                    Set(WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic
            )
          }
        }
      }
    }
  }

  it("does not match a lower version") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            // process Work V2
            val workAv2 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              version = 2,
              identifiers = List(aIdentifier)
            )

            val expectedMatchedWorkAv2 = WorkGraphIdentifiersList(
              Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/A", 2)))))

            processAndAssertMatchedWorkIs(
              workAv2,
              expectedMatchedWorkAv2,
              queue,
              storageBucket,
              topic)

            // Work V1 is sent but not matched
            val workAv1 = anUnidentifiedSierraWork.copy(
              sourceIdentifier = aIdentifier,
              identifiers = List(aIdentifier),
              version = 1)

            sendSQS(queue, storageBucket, workAv1)
            Thread.sleep(2000)
            eventually {
              noMessagesAreWaitingIn(queue)
              val snsMessages = listMessagesReceivedFromSNS(topic)
              val actualMatchedWorkLists = snsMessages.map { snsMessage =>
                fromJson[WorkGraphIdentifiersList](snsMessage.message).get
              }
              actualMatchedWorkLists.size shouldBe 1
              actualMatchedWorkLists shouldBe List(expectedMatchedWorkAv2)
            }
          }
        }
      }
    }
  }

  private def processAndAssertMatchedWorkIs(
    workToMatch: UnidentifiedWork,
    expectedMatchedWorks: WorkGraphIdentifiersList,
    queue: SQS.Queue,
    storageBucket: Bucket,
    topic: Topic): Any = {
    sendSQS(queue, storageBucket, workToMatch)
    eventually {
      assertMatchedResultIncludes(
        topic,
        expectedMatchedWorks
      )
    }
  }

  private def assertMatchedResultIncludes(
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

  private def hybridRecordNotificationMessage(message: String,
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
