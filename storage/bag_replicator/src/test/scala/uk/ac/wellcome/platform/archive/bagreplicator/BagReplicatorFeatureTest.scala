package uk.ac.wellcome.platform.archive.bagreplicator

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions

import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete

import scala.collection.JavaConverters._

class BagReplicatorFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with MetricsSenderFixture
    with BagReplicatorFixtures
    with ProgressUpdateAssertions {

  it("receives a notification") {
    withApp {
      case (
          sourceBucket,
          queuePair,
          destinationBucket,
          progressTopic,
          outgoingTopic) =>
        val requestId = randomUUID
        val storageSpace = randomStorageSpace
        val bagInfo = randomBagInfo
        withBagNotification(
          queuePair,
          sourceBucket,
          requestId,
          storageSpace,
          bagInfo = bagInfo) { bagLocation =>
          val sourceItems = s3Client.listObjects(
            bagLocation.storageNamespace,
            bagLocation.completePath)
          val sourceKeyEtags =
            sourceItems.getObjectSummaries.asScala.toList.map(_.getETag)

          eventually {
            val destinationItems = s3Client.listObjects(destinationBucket.name)
            val destinationKeyEtags =
              destinationItems.getObjectSummaries.asScala.toList.map(_.getETag)

            destinationKeyEtags should contain theSameElementsAs sourceKeyEtags

            assertSnsReceivesOnly(
              ArchiveComplete(
                archiveRequestId = requestId,
                bagLocation = bagLocation
              ),
              outgoingTopic
            )

            assertTopicReceivesProgressEventUpdate(requestId, progressTopic) {
              events =>
                events should have size 1
                events.head.description shouldBe s"Bag replicated successfully"
            }

          }
        }
    }
  }
}
