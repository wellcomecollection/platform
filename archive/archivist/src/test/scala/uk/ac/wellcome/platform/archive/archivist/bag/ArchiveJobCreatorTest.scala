package uk.ac.wellcome.platform.archive.archivist.bag
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerator
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveJob, BagItConfig, BagManifestLocation}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath, DigitisedStorageType}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

class ArchiveJobCreatorTest extends FunSpec with ZipBagItFixture with BagUploaderConfigGenerator with Matchers with Inside{
  it("creates an archive job"){
    withBagItZip() { case (bagIdentifier, zipFile) =>
    val bucketName = "bucket"
      inside(ArchiveJobCreator.create(zipFile, createBagUploaderConfig(Bucket(bucketName)))) {
        case Right(ArchiveJob(actualZipFile, bagLocation, bagItConfig, bagManifestLocations)) =>
          actualZipFile shouldBe zipFile
          bagLocation shouldBe BagLocation(
            bucketName,
            "archive",
            BagPath(s"$DigitisedStorageType/$bagIdentifier"))
          bagItConfig shouldBe BagItConfig()
          bagManifestLocations should contain only (BagManifestLocation("tagmanifest-sha256.txt"),BagManifestLocation("manifest-sha256.txt"))
      }
    }
  }

}
