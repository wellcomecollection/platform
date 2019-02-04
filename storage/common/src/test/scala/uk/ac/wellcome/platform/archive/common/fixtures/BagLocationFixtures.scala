package uk.ac.wellcome.platform.archive.common.fixtures

import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait BagLocationFixtures extends S3 with BagIt {
  def withBag[R](
    storageBucket: Bucket,
    dataFileCount: Int = 1,
    bagInfo: BagInfo = randomBagInfo,
    storageSpace: StorageSpace = randomStorageSpace,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createTagManifest: List[(String, String)] => Option[FileEntry] =
      createValidTagManifest)(testWith: TestWith[FuzzyWuzzy, R]): R = {
    val bagIdentifier = randomAlphanumeric()

    info(s"Creating bag $bagIdentifier")

    val fileEntries = createBag(
      bagInfo,
      dataFileCount = dataFileCount,
      createDataManifest = createDataManifest,
      createTagManifest = createTagManifest)
    val storagePrefix = "archive"

    val bagLocation = FuzzyWuzzy(
      storageNamespace = storageBucket.name,
      storagePrefix = storagePrefix,
      storageSpace = storageSpace,
      bagPath = BagPath(bagIdentifier)
    )

    fileEntries.map((entry: FileEntry) => {
      s3Client
        .putObject(
          bagLocation.storageNamespace,
          s"${bagLocation.completeFilepath}/${entry.name}",
          entry.contents
        )
    })

    testWith(bagLocation)
  }
}
