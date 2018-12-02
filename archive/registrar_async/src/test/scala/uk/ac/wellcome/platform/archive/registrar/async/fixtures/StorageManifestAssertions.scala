package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import java.time.Instant

import org.scalatest.{Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.models.{BagInfo, Namespace}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  StorageLocation,
  StorageProvider
}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

trait StorageManifestAssertions extends Inside with Matchers {
  def assertStorageManifest(storageManifest: StorageManifest)(
    expectedStorageSpace: Namespace,
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

        provider shouldBe StorageProvider(id = "aws-s3-ia")

        actualNamespace shouldBe expectedNamespace
        actualPath shouldBe expectedPath

        createdDate.isAfter(createdDateAfter) shouldBe true
    }
  }
}
