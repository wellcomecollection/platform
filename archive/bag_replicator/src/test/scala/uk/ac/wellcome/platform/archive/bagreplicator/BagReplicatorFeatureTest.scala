package uk.ac.wellcome.platform.archive.bagreplicator

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.bagreplicator.fixtures.BagReplicatorFixtures
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import scala.collection.JavaConverters._

class BagReplicatorFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with RandomThings
    with MetricsSenderFixture
    with BagReplicatorFixtures {

  it("receives a notification") {
    withBagReplicator {
      case (sourceBucket, queuePair, destinationBucket, progressTopic) =>
        val requestId = randomUUID
        val storageSpace = randomStorageSpace
        val bagInfo = randomBagInfo
        withBagNotification(
          queuePair,
          sourceBucket,
          requestId,
          storageSpace,
          bagInfo = bagInfo) { bagLocation =>

          val sourceItems = s3Client.listObjects(bagLocation.storageNamespace, bagLocation.bagPathInStorage)
          val sourceKeys = sourceItems.getObjectSummaries.asScala.toList.map(_.getETag)

          eventually {
            val bagName = bagLocation.bagPath.value.split("/").last
            val destinationBagPath = s"storage-root/space/$bagName"
            val destinationItems = s3Client.listObjects(destinationBucket.name, destinationBagPath)
            val destinationKeys = destinationItems.getObjectSummaries.asScala.toList.map(_.getETag)

            destinationKeys should contain theSameElementsAs sourceKeys
          }
        }
    }
  }
}
