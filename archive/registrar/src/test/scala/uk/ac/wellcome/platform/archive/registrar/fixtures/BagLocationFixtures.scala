package uk.ac.wellcome.platform.archive.registrar.fixtures
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath, DigitisedStorageType}
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait BagLocationFixtures extends S3 with BagIt {
  def withBag[R](storageBucket: Bucket, dataFileCount: Int = 1, createDataManifest: List[(String, String)] => Option[FileEntry] =
  createValidDataManifest)(
    testWith: TestWith[BagLocation, R]) = {
    val bagIdentifier = randomAlphanumeric()

    info(s"Creating bag $bagIdentifier")

    val fileEntries = createBag(bagIdentifier, dataFileCount, createDataManifest = createDataManifest)
    val storagePrefix = "archive"

    val bagLocation = BagLocation(
      storageBucket.name,
      storagePrefix,
      BagPath(s"$DigitisedStorageType/$bagIdentifier"))

    fileEntries.map((entry: FileEntry) => {
      s3Client
        .putObject(
          bagLocation.storageNamespace,
          s"$storagePrefix/${bagLocation.bagPath}/${entry.name}",
          entry.contents
        )
    })

    testWith(bagLocation)
  }
}
