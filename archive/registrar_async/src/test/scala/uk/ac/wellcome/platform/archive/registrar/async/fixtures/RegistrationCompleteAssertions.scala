package uk.ac.wellcome.platform.archive.registrar.async.fixtures
import java.time.Instant

import org.scalatest.{Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.models.BagId
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

trait RegistrationCompleteAssertions extends Inside with Matchers {
  def assertRegistrationComplete(
                                 storageManifest: StorageManifest)(
                                 expectedBagId: BagId,
                                 expectedNamespace: String,
                                 expectedPath: String,
                                 filesNumber: Long,
                                 createdDateAfter: Instant): Unit = {
    inside(storageManifest) {

      case StorageManifest(
          actualBagId,
          FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
          Location(provider, ObjectLocation(actualNamespace, actualPath)),
          createdDate) =>
        actualBagId shouldBe expectedBagId

        bagDigestFiles should have size filesNumber

        provider shouldBe Provider(id = "aws-s3-ia", label = "AWS S3 - Infrequent Access")

        actualNamespace shouldBe expectedNamespace
        actualPath shouldBe expectedPath

        createdDate.isAfter(createdDateAfter) shouldBe true
    }
  }
}
