package uk.ac.wellcome.platform.archive.registrar.async.factories

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.common.fixtures.{FileEntry, RandomThings}
import uk.ac.wellcome.platform.archive.common.generators.ArchiveCompleteGenerators
import uk.ac.wellcome.platform.archive.common.models.error.{
  DownloadError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath}
import uk.ac.wellcome.platform.archive.registrar.async.fixtures.BagLocationFixtures
import uk.ac.wellcome.platform.archive.registrar.common.models._

class StorageManifestFactoryTest
    extends FunSpec
    with ArchiveCompleteGenerators
    with BagLocationFixtures
    with RandomThings
    with Inside {
  implicit val _ = s3Client

  it("returns a right of storage manifest if reading a bag location succeeds") {
    val storageSpace = randomStorageSpace
    withLocalS3Bucket { bucket =>
      withBag(bucket) {
        case (bagLocation, bagInfo) =>
          val archiveComplete = createArchiveCompleteWith(
            space = storageSpace,
            bagLocation = bagLocation
          )

          val storageManifest =
            StorageManifestFactory.create(archiveComplete)

          inside(storageManifest) {
            case Right(
                StorageManifest(
                  actualStorageSpace,
                  actualBagInfo,
                  FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
                  FileManifest(
                    ChecksumAlgorithm("sha256"),
                    tagManifestDigestFiles),
                  _,
                  _)) =>
              actualStorageSpace shouldBe storageSpace
              actualBagInfo shouldBe bagInfo
              bagDigestFiles should have size 1
              tagManifestDigestFiles should have size 3
              tagManifestDigestFiles.map(_.path.value) should contain theSameElementsAs List(
                "manifest-sha256.txt",
                "bag-info.txt",
                "bagit.txt")
          }
      }
    }
  }

  describe("returning a left of registrar error ...") {
    it("if no files are at the BagLocation") {
      withLocalS3Bucket { bucket =>
        val bagLocation =
          BagLocation(bucket.name, "archive", BagPath(s"space/b1234567"))
        val archiveComplete = createArchiveCompleteWith(
          bagLocation = bagLocation
        )
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
      withLocalS3Bucket { bucket =>
        withBag(
          bucket,
          createDataManifest =
            _ => Some(FileEntry("manifest-sha256.txt", "bleeergh!"))) {
          case (bagLocation, _) =>
            val archiveComplete = createArchiveCompleteWith(
              bagLocation = bagLocation
            )
            val value = StorageManifestFactory.create(archiveComplete)
            value shouldBe Left(
              InvalidBagManifestError(archiveComplete, "manifest-sha256.txt"))
        }
      }
    }

    it("if the BagLocation has an invalid tagmanifest") {
      withLocalS3Bucket { bucket =>
        withBag(
          bucket,
          createTagManifest =
            _ => Some(FileEntry("tagmanifest-sha256.txt", "blaaargh!"))) {
          case (bagLocation, _) =>
            val archiveComplete = createArchiveCompleteWith(
              bagLocation = bagLocation
            )
            val value = StorageManifestFactory.create(archiveComplete)
            value shouldBe Left(
              InvalidBagManifestError(
                archiveComplete,
                "tagmanifest-sha256.txt"))
        }
      }
    }
  }
}
