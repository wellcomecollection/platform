package uk.ac.wellcome.platform.archive.bagreplicator

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.ReplicationResult
import uk.ac.wellcome.platform.archive.common.progress.ProgressUpdateAssertions

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
          dstRootPath,
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
          bagInfo = bagInfo) { srcBagLocation =>
          eventually {
            val dstBagLocation = srcBagLocation.copy(
              storageNamespace = destinationBucket.name,
              storagePrefix = dstRootPath
            )

            verifyBagCopied(srcBagLocation, dstBagLocation)
            assertSnsReceivesOnly(
              ReplicationResult(
                archiveRequestId = requestId,
                srcBagLocation = srcBagLocation,
                dstBagLocation = dstBagLocation
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
