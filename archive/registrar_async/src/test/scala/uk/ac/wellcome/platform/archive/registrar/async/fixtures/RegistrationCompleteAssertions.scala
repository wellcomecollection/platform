package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import java.time.Instant

import org.scalatest.{Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.models.{BagId, BagLocation}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait RegistrationCompleteAssertions extends Inside with Matchers {
  def assertRegistrationComplete(storageBucket: Bucket,
                                 bagLocation: BagLocation,
                                 expectedBagId: BagId,
                                 storageManifest: StorageManifest,
                                 filesNumber: Long,createdDateAfter: Instant): Unit = {
    inside(storageManifest) {

      case StorageManifest(
          actualBagId,
          FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
          createdDate) =>
        actualBagId shouldBe expectedBagId

        bagDigestFiles should have size filesNumber

        createdDate.isAfter(createdDateAfter) shouldBe true
    }
  }
}
