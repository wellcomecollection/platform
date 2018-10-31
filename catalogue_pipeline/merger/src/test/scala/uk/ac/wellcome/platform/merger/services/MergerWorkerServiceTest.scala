package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import org.mockito.Matchers.any
import org.mockito.Mockito.{atLeastOnce, times, verify}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.merger.fixtures.{
  LocalWorksVhs,
  MatcherResultFixture
}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global

class MergerWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with SQS
    with Akka
    with IntegrationPatience
    with MetricsSenderFixture
    with LocalVersionedHybridStore
    with SNS
    with Messaging
    with WorksGenerators
    with LocalWorksVhs
    with MatcherResultFixture
    with Matchers
    with MockitoSugar {
  case class TestObject(something: String)

  it(
    "reads matcher result messages, retrieves the works from vhs and sends them to sns") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, metricsSender) =>
        val work1 = createUnidentifiedWork
        val work2 = createUnidentifiedWork
        val work3 = createUnidentifiedWork

        val matcherResult =
          matcherResultWith(Set(Set(work3), Set(work1, work2)))

        givenStoredInVhs(vhs, entries = List(work1, work2, work3))

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = getMessages[BaseWork](topic)
          worksSent should contain only (work1, work2, work3)

          verify(metricsSender, atLeastOnce)
            .countSuccess(any[String])
        }
    }
  }

  it("sends InvisibleWorks unmerged") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, metricsSender) =>
        val work = createUnidentifiedInvisibleWork

        val matcherResult = matcherResultWith(Set(Set(work)))

        givenStoredInVhs(vhs, work)

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = getMessages[BaseWork](topic)
          worksSent should contain only work

          verify(metricsSender, times(1))
            .countSuccess(any[String])
        }
    }
  }

  it("fails if the work is not in vhs") {
    withMergerWorkerServiceFixtures {
      case (_, QueuePair(queue, dlq), topic, metricsSender) =>
        val work = createUnidentifiedWork

        val matcherResult = matcherResultWith(Set(Set(work)))

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          listMessagesReceivedFromSNS(topic) shouldBe empty

          verify(metricsSender, times(3))
            .countFailure(any[String])
        }
    }
  }

  it("discards works with newer versions in vhs, sends along the others") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, _) =>
        val work = createUnidentifiedWork
        val olderWork = createUnidentifiedWork
        val newerWork = olderWork.copy(version = 2)

        val matcherResult = matcherResultWith(Set(Set(work, olderWork)))

        givenStoredInVhs(
          vhs,
          entries = List[TransformedBaseWork](work, newerWork))

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
          val worksSent = getMessages[BaseWork](topic)
          worksSent should contain only work
        }
    }
  }

  it("discards works with version 0 and sends along the others") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, metricsSender) =>
        val versionZeroWork = createUnidentifiedWorkWith(version = 0)
        val work = versionZeroWork
          .copy(version = 1)

        val matcherResult = matcherResultWith(Set(Set(work, versionZeroWork)))

        givenStoredInVhs(vhs, work)

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = getMessages[BaseWork](topic)
          worksSent should contain only work

          verify(metricsSender, times(1))
            .countSuccess(any[String])
        }
    }
  }

  it("sends a merged work and a redirected work to SQS") {
    val physicalWork = createSierraPhysicalWork
    val digitalWork = createSierraDigitalWork

    val works = List(physicalWork, digitalWork)

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, metricsSender) =>
        givenStoredInVhs(vhs, works)

        val matcherResult = MatcherResult(
          Set(
            MatchedIdentifiers(worksToWorkIdentifiers(works))
          )
        )

        sendNotificationToSQS(queue = queue, message = matcherResult)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = getMessages[BaseWork](topic).distinct
          worksSent should have size 2

          val redirectedWorks = worksSent.collect {
            case work: UnidentifiedRedirectedWork => work
          }
          val mergedWorks = worksSent.collect {
            case work: UnidentifiedWork => work
          }

          redirectedWorks should have size 1
          redirectedWorks.head.sourceIdentifier shouldBe digitalWork.sourceIdentifier
          redirectedWorks.head.redirect shouldBe IdentifiableRedirect(
            physicalWork.sourceIdentifier)

          mergedWorks should have size 1
          mergedWorks.head.sourceIdentifier shouldBe physicalWork.sourceIdentifier
        }
    }
  }

  it("splits the received works into multiple merged works if required") {
    val workPair1 = List(createSierraPhysicalWork, createSierraDigitalWork)
    val workPair2 = List(createSierraPhysicalWork, createSierraDigitalWork)

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), topic, metricsSender) =>
        givenStoredInVhs(vhs, entries = workPair1 ++ workPair2)

        val matcherResult = MatcherResult(
          Set(
            MatchedIdentifiers(worksToWorkIdentifiers(workPair1)),
            MatchedIdentifiers(worksToWorkIdentifiers(workPair2))
          ))

        sendNotificationToSQS(queue = queue, message = matcherResult)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = getMessages[BaseWork](topic).distinct
          worksSent should have size 4

          val redirectedWorks = worksSent.collect {
            case work: UnidentifiedRedirectedWork => work
          }
          val mergedWorks = worksSent.collect {
            case work: UnidentifiedWork => work
          }

          redirectedWorks should have size 2
          mergedWorks should have size 2
        }
    }
  }

  it("fails if the message sent is not a matcher result") {
    withMergerWorkerServiceFixtures {
      case (_, QueuePair(queue, dlq), _, metricsSender) =>
        sendNotificationToSQS(
          queue = queue,
          message = TestObject("not-a-matcher-result")
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          verify(metricsSender, times(3))
            .countRecognisedFailure(any[String])
        }
    }
  }

  def withMergerWorkerServiceFixtures[R](
    testWith: TestWith[(VersionedHybridStore[TransformedBaseWork,
                                             EmptyMetadata,
                                             ObjectStore[TransformedBaseWork]],
                        QueuePair,
                        Topic,
                        MetricsSender),
                       R]): R = {
    withLocalS3Bucket { storageBucket =>
      withLocalS3Bucket { messageBucket =>
        withLocalDynamoDbTable { table =>
          withTypeVHS[TransformedBaseWork, EmptyMetadata, R](
            storageBucket,
            table) { vhs =>
            withActorSystem { actorSystem =>
              withLocalSqsQueueAndDlq {
                case queuePair @ QueuePair(queue, dlq) =>
                  withLocalSnsTopic { topic =>
                    withMockMetricSender { metricsSender =>
                      withSQSStream[NotificationMessage, R](
                        actorSystem,
                        queue,
                        metricsSender) { sqsStream =>
                        withMessageWriter[BaseWork, R](messageBucket, topic) {
                          snsWriter =>
                            withMergerWorkerService(
                              actorSystem,
                              sqsStream,
                              vhs,
                              snsWriter) { _ =>
                              testWith((vhs, queuePair, topic, metricsSender))
                            }
                        }
                      }
                    }
                  }
              }
            }
          }
        }
      }
    }
  }

  private def withMergerWorkerService[R](
    actorSystem: ActorSystem,
    sqsStream: SQSStream[NotificationMessage],
    vhs: VersionedHybridStore[TransformedBaseWork,
                              EmptyMetadata,
                              ObjectStore[TransformedBaseWork]],
    messageWriter: MessageWriter[BaseWork])(
    testWith: TestWith[MergerWorkerService, R]) = {
    testWith(
      new MergerWorkerService(
        actorSystem,
        sqsStream,
        playbackService = new RecorderPlaybackService(vhs),
        mergerManager = new MergerManager(new Merger()),
        messageWriter))
  }
}
