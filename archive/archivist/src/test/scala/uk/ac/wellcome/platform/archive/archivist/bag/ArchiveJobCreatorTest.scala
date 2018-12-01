package uk.ac.wellcome.platform.archive.archivist.bag

import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerators
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveJob,
  BagItConfig,
  BagManifestLocation
}
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

class ArchiveJobCreatorTest
    extends FunSpec
    with ZipBagItFixture
    with BagUploaderConfigGenerators
    with Matchers
    with Inside
    with IngestBagRequestGenerators {
  it("creates an archive job") {
    withBagItZip() { zipFile =>
      val bucketName = "bucket"
      val ingestRequest = createIngestBagRequest
      inside(
        ArchiveJobCreator
          .create(
            zipFile,
            createBagUploaderConfigWith(Bucket(bucketName)),
            ingestRequest
          )) {
        case Right(
        ArchiveJob(
        bagIdentifier,
        actualZipFile,
        bagLocation,
        bagItConfig,
        bagManifestLocations)) =>
          actualZipFile shouldBe zipFile
          bagLocation shouldBe BagLocation(
            bucketName,
            "archive",
            BagPath(s"${ingestRequest.storageSpace}/$bagIdentifier"))
          bagItConfig shouldBe BagItConfig()
          bagManifestLocations should contain only(BagManifestLocation(
            "tagmanifest-sha256.txt"), BagManifestLocation(
            "manifest-sha256.txt"))
      }
    }
  }
}
