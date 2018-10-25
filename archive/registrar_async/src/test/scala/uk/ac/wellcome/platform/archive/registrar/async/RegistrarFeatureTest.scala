package uk.ac.wellcome.platform.archive.registrar.async

import java.time.Instant

import org.scalatest.concurrent.{
  IntegrationPatience,
  PatienceConfiguration,
  ScalaFutures
}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  Resource,
  ResourceIdentifier
}
import uk.ac.wellcome.platform.archive.registrar.async.fixtures.StorageManifestAssertions
import uk.ac.wellcome.platform.archive.registrar.async.fixtures.RegistrarFixtures
import uk.ac.wellcome.storage.dynamo._

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with RegistrarFixtures
    with Inside
    with RandomThings
    with ProgressUpdateAssertions
    with StorageManifestAssertions
    with PatienceConfiguration {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  implicit val _ = s3Client

  it(
    "registers an archived BagIt bag from S3 and notifies the progress tracker") {
    withRegistrar {
      case (
          storageBucket,
          queuePair,
          progressTopic,
          registrar,
          vhs
          ) =>
        val requestId = randomUUID
        val createdAfterDate = Instant.now()

        withBagNotification(
          requestId,
          queuePair,
          storageBucket
        ) {
          case (bagLocation, bagInfo, bagId) =>
            registrar.run()

            eventually {
              val futureMaybeManifest = vhs.getRecord(bagId.toString)

              whenReady(futureMaybeManifest) { maybeStorageManifest =>
                maybeStorageManifest shouldBe defined

                val storageManifest = maybeStorageManifest.get

                assertStorageManifest(storageManifest)(
                  expectedStorageSpace = bagId.space,
                  expectedBagInfo = bagInfo,
                  expectedNamespace = storageBucket.name,
                  expectedPath =
                    s"${bagLocation.storagePath}/${bagLocation.bagPath.value}",
                  filesNumber = 1,
                  createdDateAfter = createdAfterDate
                )

                assertTopicReceivesProgressStatusUpdate(
                  requestId,
                  progressTopic,
                  Progress.Completed,
                  List(Resource(ResourceIdentifier(bagId.toString)))) {
                  events =>
                    events should have size 1
                    events.head.description shouldBe "Bag registered successfully"
                }
              }
            }
        }
    }
  }

  it("notifies the progress tracker if registering a bag fails") {
    withRegistrar {
      case (storageBucket, queuePair, progressTopic, registrar, vhs) =>
        val requestId = randomUUID
        val bagId = randomBagId

        val bagLocation = BagLocation(
          storageBucket.name,
          "archive",
          BagPath(s"space/does-not-exist"))

        sendNotificationToSQS(
          queuePair.queue,
          ArchiveComplete(requestId, bagId.space, bagLocation)
        )

        registrar.run()

        eventually {
          val futureMaybeManifest = vhs.getRecord(bagId.toString)

          whenReady(futureMaybeManifest) { maybeStorageManifest =>
            maybeStorageManifest shouldNot be(defined)
          }

          assertTopicReceivesProgressStatusUpdate(
            requestId,
            progressTopic,
            Progress.Failed,
            Nil) { events =>
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
          progressTopic,
          registrar,
          _) =>
        val requestId1 = randomUUID
        val requestId2 = randomUUID

        withBagNotification(requestId1, queuePair, storageBucket) { _ =>
          withBagNotification(requestId2, queuePair, storageBucket) { _ =>
            registrar.run()

            eventually {
              listMessagesReceivedFromSNS(progressTopic) shouldBe empty

              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 2)
            }
          }
        }
    }
  }
}
