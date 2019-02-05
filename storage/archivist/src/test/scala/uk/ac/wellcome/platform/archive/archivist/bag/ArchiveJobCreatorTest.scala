package uk.ac.wellcome.platform.archive.archivist.bag

import java.util.zip.ZipFile

import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.BagUploaderConfigGenerators
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveJob,
  BagItConfig
}
import uk.ac.wellcome.platform.archive.common.generators.IngestBagRequestGenerators
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemPath,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.storage.fixtures.S3.Bucket

class ArchiveJobCreatorTest
    extends FunSpec
    with ZipBagItFixture
    with BagUploaderConfigGenerators
    with Matchers
    with Inside
    with IngestBagRequestGenerators {
  it("creates an archive job") {
    withBagItZip() { file =>
      val bucketName = "bucket"
      val ingestRequest = createIngestBagRequest
      inside(
        ArchiveJobCreator
          .create(
            new ZipFile(file),
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
          actualZipFile.size() shouldBe (new ZipFile(file)).size()
          bagLocation shouldBe BagLocation(
            storageNamespace = bucketName,
            storagePrefix = "archive",
            storageSpace = ingestRequest.storageSpace,
            bagPath = BagPath(bagIdentifier.toString)
          )
          bagItConfig shouldBe BagItConfig()
          bagManifestLocations should contain only (BagItemPath(
            "tagmanifest-sha256.txt"), BagItemPath("manifest-sha256.txt"))
      }
    }
  }
}
