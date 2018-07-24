package uk.ac.wellcome.platform.matcher.messages

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier
}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

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
              MatcherResult(
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
    "sends an invisible work as a single matched result with no other matched identifiers") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            val invisibleWork = createUnidentifiedInvisibleWork
            val workId =
              s"${invisibleWork.sourceIdentifier.identifierType.id}/${invisibleWork.sourceIdentifier.value}"
            val expectedMatchedWorks =
              MatcherResult(
                Set(
                  MatchedIdentifiers(
                    Set(WorkIdentifier(identifier = workId, version = 1))
                  ))
              )

            sendSQS(queue, storageBucket, invisibleWork)
            eventually {
              assertLastMatchedResultIs(
                topic,
                expectedMatchedWorks
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
          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            // Work Av1
            val workAv1 =
              createUnidentifiedWorkWith(
                sourceIdentifier = aIdentifier,
                mergeCandidates = List(MergeCandidate(bIdentifier)))
            // Work Av1 matched to B (before B exists hence version 0)
            // need to match to works that do not exist to support
            // bi-directionally matched works without deadlocking (A->B, B->A)
            val expectedMatchedWorks = MatcherResult(
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
            val workAv1 =
              createUnidentifiedWorkWith(sourceIdentifier = aIdentifier)

            val expectedMatchedWorks = MatcherResult(
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
            val workBv1 =
              createUnidentifiedWorkWith(sourceIdentifier = bIdentifier)

            processAndAssertMatchedWorkIs(
              workBv1,
              MatcherResult(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic)

            // Work Av1 matched to B
            val workAv2 = createUnidentifiedWorkWith(
              sourceIdentifier = aIdentifier,
              version = 2,
              mergeCandidates = List(MergeCandidate(bIdentifier)))

            processAndAssertMatchedWorkIs(
              workAv2,
              MatcherResult(
                Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 2),
                    WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic
            )

            // Work Cv1
            val workCv1 =
              createUnidentifiedWorkWith(sourceIdentifier = cIdentifier)

            processAndAssertMatchedWorkIs(
              workCv1,
              MatcherResult(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/C", 1))))),
              queue,
              storageBucket,
              topic)

            // Work Bv2 matched to C
            val workBv2 = createUnidentifiedWorkWith(
              sourceIdentifier = bIdentifier,
              version = 2,
              mergeCandidates = List(MergeCandidate(cIdentifier)))

            processAndAssertMatchedWorkIs(
              workBv2,
              MatcherResult(
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
            val workAv1 = createUnidentifiedWorkWith(
              sourceIdentifier = aIdentifier,
              version = 1)

            processAndAssertMatchedWorkIs(
              workAv1,
              MatcherResult(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/A", 1))))),
              queue,
              storageBucket,
              topic)

            // Work Bv1
            val workBv1 = createUnidentifiedWorkWith(
              sourceIdentifier = bIdentifier,
              version = 1)

            processAndAssertMatchedWorkIs(
              workBv1,
              MatcherResult(Set(MatchedIdentifiers(
                Set(WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic)

            // Match Work A to Work B
            val workAv2MatchedToB = createUnidentifiedWorkWith(
              sourceIdentifier = aIdentifier,
              version = 2,
              mergeCandidates = List(MergeCandidate(bIdentifier)))

            processAndAssertMatchedWorkIs(
              workAv2MatchedToB,
              MatcherResult(
                Set(
                  MatchedIdentifiers(Set(
                    WorkIdentifier("sierra-system-number/A", 2),
                    WorkIdentifier("sierra-system-number/B", 1))))),
              queue,
              storageBucket,
              topic
            )

            // A no longer matches B
            val workAv3WithNoMatchingWorks = createUnidentifiedWorkWith(
              sourceIdentifier = aIdentifier,
              version = 3)

            processAndAssertMatchedWorkIs(
              workAv3WithNoMatchingWorks,
              MatcherResult(
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
      withLocalSqsQueueAndDlq { queuePair =>
        withLocalS3Bucket { storageBucket =>
          withMatcherMessageReceiver(queuePair.queue, storageBucket, topic) {
            _ =>
              // process Work V2
              val workAv2 = createUnidentifiedWorkWith(
                sourceIdentifier = aIdentifier,
                version = 2
              )

              val expectedMatchedWorkAv2 = MatcherResult(
                Set(MatchedIdentifiers(
                  Set(WorkIdentifier("sierra-system-number/A", 2)))))

              processAndAssertMatchedWorkIs(
                workAv2,
                expectedMatchedWorkAv2,
                queuePair.queue,
                storageBucket,
                topic)

              // Work V1 is sent but not matched
              val workAv1 = createUnidentifiedWorkWith(
                sourceIdentifier = aIdentifier,
                version = 1)

              sendSQS(queuePair.queue, storageBucket, workAv1)
              eventually {
                noMessagesAreWaitingIn(queuePair.queue)
                noMessagesAreWaitingIn(queuePair.dlq)
                assertLastMatchedResultIs(topic, expectedMatchedWorkAv2)
              }
          }
        }
      }
    }
  }

  it("does not match an existing version with different information") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueueAndDlq {
        case QueuePair(queue, dlq) =>
          withLocalS3Bucket { storageBucket =>
            withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
              val workAv2 = createUnidentifiedWorkWith(
                sourceIdentifier = aIdentifier,
                version = 2
              )

              val expectedMatchedWorkAv2 = MatcherResult(
                Set(MatchedIdentifiers(
                  Set(WorkIdentifier("sierra-system-number/A", 2)))))

              processAndAssertMatchedWorkIs(
                workAv2,
                expectedMatchedWorkAv2,
                queue,
                storageBucket,
                topic)

              // Work V1 is sent but not matched
              val differentWorkAv2 = createUnidentifiedWorkWith(
                sourceIdentifier = aIdentifier,
                mergeCandidates = List(MergeCandidate(bIdentifier)),
                version = 2)

              sendSQS(queue, storageBucket, differentWorkAv2)
              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
              }
            }
          }
      }
    }
  }

  private def processAndAssertMatchedWorkIs(workToMatch: UnidentifiedWork,
                                            expectedMatchedWorks: MatcherResult,
                                            queue: SQS.Queue,
                                            storageBucket: Bucket,
                                            topic: Topic): Any = {
    sendSQS(queue, storageBucket, workToMatch)
    eventually {
      assertLastMatchedResultIs(
        topic,
        expectedMatchedWorks
      )
    }
  }

  private def assertLastMatchedResultIs(topic: Topic,
                                        identifiersList: MatcherResult) = {

    val snsMessages = listMessagesReceivedFromSNS(topic)
    snsMessages.size should be >= 1

    val actualMatchedWorkLists = snsMessages.map { snsMessage =>
      fromJson[MatcherResult](snsMessage.message).get
    }
    actualMatchedWorkLists.last shouldBe identifiersList
  }

  private def sendSQS(queue: SQS.Queue,
                      storageBucket: Bucket,
                      work: TransformedBaseWork) = {
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

    createNotificationMessageWith(message = hybridRecord)
  }
}
