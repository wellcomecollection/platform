package uk.ac.wellcome.platform.archive.registrar.async.factories

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.scalatest.{FunSpec, Inside}
import uk.ac.wellcome.platform.archive.common.fixtures.{
  BagLocationFixtures,
  FileEntry,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.generators.ArchiveCompleteGenerators
import uk.ac.wellcome.platform.archive.common.models.bagit
import uk.ac.wellcome.platform.archive.common.models.error.{
  DownloadError,
  InvalidBagManifestError
}
import uk.ac.wellcome.platform.archive.registrar.common.models._

class StorageManifestFactoryTest
    extends FunSpec
    with ArchiveCompleteGenerators
    with BagLocationFixtures
    with RandomThings
    with Inside {
  implicit val _ = s3Client

  it("returns a right of storage manifest if reading a bag location succeeds") {
    withLocalS3Bucket { bucket =>
      val bagInfo = randomBagInfo
      withBag(bucket, bagInfo = bagInfo) { bagLocation =>
        val archiveComplete = createArchiveCompleteWith(
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
            actualStorageSpace shouldBe bagLocation.storageSpace
            actualBagInfo shouldBe bagInfo
            bagDigestFiles should have size 1
            tagManifestDigestFiles should have size 3
            tagManifestDigestFiles.map { _.path.toString } should contain theSameElementsAs List(
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
        val bagLocation = bagit.BagLocation(
          storageNamespace = bucket.name,
          storagePrefix = "archive",
          storageSpace = randomStorageSpace,
          bagPath = randomBagPath
        )
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
          bagLocation =>
            val archiveComplete = createArchiveCompleteWith(
              bagLocation = bagLocation
            )
            val value = StorageManifestFactory.create(archiveComplete)
            inside(value) {
              case Left(
                  InvalidBagManifestError(
                    actualArchiveComplete,
                    "manifest-sha256.txt",
                    _)) =>
                actualArchiveComplete shouldBe archiveComplete
            }
        }
      }
    }

    it("if the BagLocation has an invalid tagmanifest") {
      withLocalS3Bucket { bucket =>
        withBag(
          bucket,
          createTagManifest =
            _ => Some(FileEntry("tagmanifest-sha256.txt", "blaaargh!"))) {
          bagLocation =>
            val archiveComplete = createArchiveCompleteWith(
              bagLocation = bagLocation
            )
            val value = StorageManifestFactory.create(archiveComplete)
            inside(value) {
              case Left(
                  InvalidBagManifestError(
                    actualArchiveComplete,
                    "tagmanifest-sha256.txt",
                    _)) =>
                actualArchiveComplete shouldBe archiveComplete
            }
        }
      }
    }
  }
}
