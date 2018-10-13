package uk.ac.wellcome.platform.archive.registrar

import org.scalatest.concurrent.{
  IntegrationPatience,
  PatienceConfiguration,
  ScalaFutures
}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.registrar.fixtures.{
  RegistrationCompleteAssertions,
  Registrar => RegistrarFixture
}
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.dynamo._

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with RegistrarFixture
    with Inside
    with RandomThings
    with ProgressUpdateAssertions
    with RegistrationCompleteAssertions
    with PatienceConfiguration {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  implicit val _ = s3Client

  it(
    "registers an archived BagIt bag from S3 and notifies the progress monitor") {
    withRegistrar {
      case (
          storageBucket,
          queuePair,
          ddsTopic,
          progressTopic,
          registrar,
          vhs
          ) =>
        val requestId = randomUUID
        val bagId = randomBagId

        withBagNotification(
          requestId,
          bagId,
          queuePair,
          storageBucket
        ) { bagLocation =>
          registrar.run()

          eventually {
            val messages = listMessagesReceivedFromSNS(ddsTopic)
            messages should have size 1

            val registrationCompleteNotification =
              fromJson[RegistrationCompleteNotification](messages.head.message).get

            val bagId = registrationCompleteNotification.bagId

            val futureMaybeManifest = vhs.getRecord(bagId.toString)

            whenReady(futureMaybeManifest) { maybeStorageManifest =>
              maybeStorageManifest shouldNot be(empty)

              val storageManifest = maybeStorageManifest.get

              assertRegistrationComplete(
                storageBucket,
                bagLocation,
                bagId,
                storageManifest,
                filesNumber = 1L
              )

              assertTopicReceivesProgressUpdate(
                requestId,
                progressTopic,
                Progress.Completed) { events =>
                events should have size 1
                events.head.description shouldBe "Bag registered successfully"
              }
            }
          }
        }
    }
  }

  it("notifies the progress monitor if registering a bag fails") {
    withRegistrar {
      case (
          storageBucket,
          queuePair,
          ddsTopic,
          progressTopic,
          registrar,
          vhs) =>
        val requestId = randomUUID
        val bagId = randomBagId

        val bagLocation = BagLocation(
          storageBucket.name,
          "archive",
          BagPath(s"space/does-not-exist"))

        sendNotificationToSQS(
          queuePair.queue,
          ArchiveComplete(requestId, bagId, bagLocation)
        )

        registrar.run()

        eventually {
          val messages = listMessagesReceivedFromSNS(ddsTopic)
          messages shouldBe empty

          val futureMaybeManifest = vhs.getRecord(bagId.toString)

          whenReady(futureMaybeManifest) { maybeStorageManifest =>
            maybeStorageManifest shouldBe empty
          }

          assertTopicReceivesProgressUpdate(
            requestId,
            progressTopic,
            Progress.Failed) { events =>
            events should have size 1
            events.head.description should startWith(
              "There was an exception while downloading object")
          }
        }
    }
  }

  it("discards messages if it fails writing to the VHS") {
    withRegistrarAndBrokenVHS {
      case (
          storageBucket,
          queuePair @ QueuePair(queue, dlq),
          ddsTopic,
          progressTopic,
          registrar,
          _) =>
        val requestId1 = randomUUID
        val requestId2 = randomUUID

        val bagId1 = randomBagId
        val bagId2 = randomBagId

        withBagNotification(requestId1, bagId1, queuePair, storageBucket) {
          bagLocation1 =>
            withBagNotification(requestId2, bagId2, queuePair, storageBucket) {
              bagLocation2 =>
                registrar.run()

                eventually {
                  listMessagesReceivedFromSNS(ddsTopic) shouldBe empty
                  listMessagesReceivedFromSNS(progressTopic) shouldBe empty

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, 2)
                }
            }
        }
    }
  }
}
