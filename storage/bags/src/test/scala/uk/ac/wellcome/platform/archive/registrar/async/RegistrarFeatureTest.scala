package uk.ac.wellcome.platform.archive.registrar.async

import java.time.Instant

import org.scalatest.concurrent.{
  IntegrationPatience,
  PatienceConfiguration,
  ScalaFutures
}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.bagit.{BagId, BagLocation}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.{
  InfrequentAccessStorageProvider,
  Progress,
  StorageLocation
}
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
    with PatienceConfiguration {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  implicit val _ = s3Client

  it("registers an access BagIt bag from S3 and notifies the progress tracker") {
    withRegistrar {
      case (storageBucket, queuePair, progressTopic, vhs) =>
        val requestId = randomUUID
        val storageSpace = randomStorageSpace
        val createdAfterDate = Instant.now()
        val bagInfo = randomBagInfo

        withBagNotification(
          queuePair,
          storageBucket,
          requestId,
          storageSpace,
          bagInfo = bagInfo) {
          case (archiveBagLocation, accessBagLocation) =>
            val bagId = BagId(
              space = storageSpace,
              externalIdentifier = bagInfo.externalIdentifier
            )

            eventually {
              val futureMaybeManifest = vhs.getRecord(bagId.toString)

              whenReady(futureMaybeManifest) { maybeStorageManifest =>
                maybeStorageManifest shouldBe defined

                val storageManifest = maybeStorageManifest.get

                storageManifest.space shouldBe bagId.space
                storageManifest.info shouldBe bagInfo
                storageManifest.manifest.files should have size 1

                storageManifest.accessLocation shouldBe StorageLocation(
                  provider = InfrequentAccessStorageProvider,
                  location = accessBagLocation.objectLocation
                )
                storageManifest.archiveLocations shouldBe List(
                  StorageLocation(
                    provider = InfrequentAccessStorageProvider,
                    location = archiveBagLocation.objectLocation
                  )
                )

                storageManifest.createdDate.isAfter(createdAfterDate) shouldBe true

                assertTopicReceivesProgressStatusUpdate(
                  requestId,
                  progressTopic,
                  Progress.Completed,
                  expectedBag = Some(bagId)) { events =>
                  events.size should be >= 1
                  events.head.description shouldBe "Bag registered successfully"
                }
              }
            }
        }
    }
  }

  it("notifies the progress tracker if registering a bag fails") {
    withRegistrar {
      case (storageBucket, queuePair, progressTopic, vhs) =>
        val requestId = randomUUID
        val bagId = randomBagId

        val srcBagLocation = BagLocation(
          storageNamespace = storageBucket.name,
          storagePrefix = "archive",
          storageSpace = bagId.space,
          bagPath = randomBagPath
        )

        val dstBagLocation = srcBagLocation.copy(
          storagePrefix = "access"
        )

        sendNotificationToSQS(
          queuePair.queue,
          ReplicationResult(
            archiveRequestId = requestId,
            srcBagLocation = srcBagLocation,
            dstBagLocation = dstBagLocation
          )
        )

        eventually {
          val futureMaybeManifest = vhs.getRecord(bagId.toString)

          whenReady(futureMaybeManifest) { maybeStorageManifest =>
            maybeStorageManifest shouldNot be(defined)
          }

          assertTopicReceivesProgressStatusUpdate(
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
          progressTopic,
          _) =>
        withBagNotification(queuePair, storageBucket) { _ =>
          withBagNotification(queuePair, storageBucket) { _ =>
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
