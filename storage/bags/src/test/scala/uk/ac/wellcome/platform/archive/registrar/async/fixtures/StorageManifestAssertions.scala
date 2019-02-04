package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import java.time.Instant

import org.scalatest.{Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.models.StorageSpace
import uk.ac.wellcome.platform.archive.common.models.bagit.BagInfo
import uk.ac.wellcome.platform.archive.common.progress.models.{InfrequentAccessStorageProvider, StorageLocation}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

trait StorageManifestAssertions extends Inside with Matchers {
  def assertStorageManifest(storageManifest: StorageManifest)(
    expectedStorageSpace: StorageSpace,
    expectedBagInfo: BagInfo,
    expectedNamespace: String,
    expectedPath: String,
    filesNumber: Long,
    createdDateAfter: Instant): Unit = {
    inside(storageManifest) {

      case StorageManifest(
          actualStorageSpace,
          actualBagInfo,
          FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
          FileManifest(ChecksumAlgorithm(_), _),
          StorageLocation(
            provider,
            ObjectLocation(actualNamespace, actualPath)),
          createdDate) =>
        actualStorageSpace shouldBe expectedStorageSpace
        actualBagInfo shouldBe expectedBagInfo
        bagDigestFiles should have size filesNumber

        provider shouldBe InfrequentAccessStorageProvider

        actualNamespace shouldBe expectedNamespace
        actualPath shouldBe expectedPath

        createdDate.isAfter(createdDateAfter) shouldBe true
    }
  }
}
