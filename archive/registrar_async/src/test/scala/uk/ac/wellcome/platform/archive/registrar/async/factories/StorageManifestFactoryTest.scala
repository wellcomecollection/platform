package uk.ac.wellcome.platform.archive.registrar.async.factories

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.common.fixtures.{FileEntry, RandomThings}
import uk.ac.wellcome.platform.archive.common.models.error.{
  DownloadError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.platform.archive.registrar.async.fixtures.BagLocationFixtures
import uk.ac.wellcome.platform.archive.registrar.common.models._
class StorageManifestFactoryTest
    extends FunSpec
    with BagLocationFixtures
    with RandomThings
    with Inside {
  implicit val _ = s3Client

  it("returns a right of storage manifest if reading a bag location succeeds") {
    val requestId = randomUUID

    withLocalS3Bucket { bucket =>
      withBag(bucket) {
        case (bagLocation, bagInfo, bagId) =>
          val archiveComplete =
            ArchiveComplete(requestId, bagId.space, bagLocation)

          val storageManifest =
            StorageManifestFactory.create(archiveComplete)

          inside(storageManifest) {
            case Right(
                StorageManifest(
                  actualStorageSpace,
                  actualBagInfo,
                  FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
                  _,
                  _)) =>
              actualStorageSpace shouldBe bagId.space
              actualBagInfo shouldBe bagInfo
              bagDigestFiles should have size 1

          }
      }
    }
  }

  describe("returning a left of registrar error ...") {
    it("if no files are at the BagLocation") {

      val storageSpace = randomStorageSpace
      val requestId = randomUUID

      withLocalS3Bucket { bucket =>
        val bagLocation =
          BagLocation(bucket.name, "archive", BagPath(s"space/b1234567"))
        val archiveComplete =
          ArchiveComplete(requestId, storageSpace, bagLocation)
        val value = StorageManifestFactory.create(archiveComplete)

        inside(value) {
          case Left(DownloadError(exception, _, actualArchiveComplete)) =>
            actualArchiveComplete shouldBe archiveComplete
            exception shouldBe a[AmazonS3Exception]
            exception
              .asInstanceOf[AmazonS3Exception]
              .getErrorCode shouldBe "NoSuchKey"
        }
      }
    }

    it("if the BagLocation has an invalid manifest") {
      val requestId = randomUUID

      withLocalS3Bucket { bucket =>
        withBag(
          bucket,
          createDataManifest =
            _ => Some(FileEntry("manifest-sha256.txt", "bleeergh!"))) {
          case (bagLocation, _, bagId) =>
            val archiveComplete =
              ArchiveComplete(requestId, bagId.space, bagLocation)
            val value = StorageManifestFactory.create(archiveComplete)
            value shouldBe Left(
              InvalidBagManifestError(archiveComplete, "manifest-sha256.txt"))
        }
      }
    }
  }
}
