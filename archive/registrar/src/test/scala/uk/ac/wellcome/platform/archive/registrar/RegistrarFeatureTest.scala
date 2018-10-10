package uk.ac.wellcome.platform.archive.registrar

import java.net.URI
import java.util.UUID

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
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
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath,
  DigitisedStorageType
}
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.registrar.fixtures.{
  RegistrationCompleteAssertions,
  Registrar => RegistrarFixture
}
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.vhs.HybridRecord

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with RegistrarFixture
    with Inside
    with ProgressUpdateAssertions
    with RegistrationCompleteAssertions
    with PatienceConfiguration {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  implicit val _ = s3Client

  private val callbackHost = "localhost"
  private val callbackPort = 8080

  def createCallbackUrl = {
    val requestId = UUID.randomUUID()
    (
      new URI(
        s"http://$callbackHost:$callbackPort/callback/$requestId"
      ),
      requestId)
  }

  it(
    "registers an archived BagIt bag from S3 and notifies the progress monitor") {
    withRegistrar {
      case (
          storageBucket,
          queuePair,
          ddsTopic,
          progressTopic,
          registrar,
          hybridBucket,
          hybridTable) =>
        val (callbackUrl, requestId) = createCallbackUrl

        withBagNotification(
          requestId,
          Some(callbackUrl),
          queuePair,
          storageBucket) { bagLocation =>
          registrar.run()

          eventually {
            val messages = listMessagesReceivedFromSNS(ddsTopic)
            messages should have size 1
            val registrationCompleteNotification =
              fromJson[RegistrationComplete](messages.head.message).get

            assertStored[StorageManifest](
              hybridBucket,
              hybridTable,
              registrationCompleteNotification.storageManifest.id.value,
              registrationCompleteNotification.storageManifest
            )

            assertRegistrationComplete(
              storageBucket,
              bagLocation,
              registrationCompleteNotification,
              filesNumber = 1)
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

  it("notifies the progress monitor if registering a bag fails") {
    withRegistrar {
      case (
          storageBucket,
          queuePair,
          ddsTopic,
          progressTopic,
          registrar,
          hybridBucket,
          hybridTable) =>
        val (callbackUrl, requestId) = createCallbackUrl

        val bagLocation = BagLocation(
          storageBucket.name,
          "archive",
          BagPath(s"$DigitisedStorageType/does-not-exist"))

        sendNotificationToSQS(
          queuePair.queue,
          ArchiveComplete(requestId, bagLocation, Some(callbackUrl))
        )

        registrar.run()

        eventually {
          val messages = listMessagesReceivedFromSNS(ddsTopic)
          messages shouldBe empty

          Scanamo.get[HybridRecord](dynamoDbClient)(hybridTable.name)(
            'id -> bagLocation.bagPath.value) shouldBe None

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
        val (callbackUrl1, requestId1) = createCallbackUrl
        val (callbackUrl2, requestId2) = createCallbackUrl

        withBagNotification(
          requestId1,
          Some(callbackUrl1),
          queuePair,
          storageBucket) { bagLocation1 =>
          withBagNotification(
            requestId2,
            Some(callbackUrl2),
            queuePair,
            storageBucket) { bagLocation2 =>
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
