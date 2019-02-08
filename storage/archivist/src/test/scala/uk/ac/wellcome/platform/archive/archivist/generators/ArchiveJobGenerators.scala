package uk.ac.wellcome.platform.archive.archivist.generators

import java.io.File
import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.generators.ExternalIdentifierGenerators
import uk.ac.wellcome.platform.archive.common.models.bagit._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait ArchiveJobGenerators extends ExternalIdentifierGenerators {

  def createArchiveItemJobWith(
    file: File,
    bucket: S3.Bucket,
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    s3Key: String
  ): ArchiveItemJob =
    ArchiveItemJob(
      archiveJob = createArchiveJobWith(file, bagIdentifier, bucket),
      bagItemPath = BagItemPath(s3Key)
    )

  def createArchiveDigestItemJobWith(
    file: File,
    bucket: S3.Bucket,
    digest: String = randomAlphanumeric(),
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    s3Key: String = randomAlphanumeric()
  ): ArchiveDigestItemJob =
    ArchiveDigestItemJob(
      archiveJob = createArchiveJobWith(
        file = file,
        bagIdentifier = bagIdentifier,
        bucket = bucket
      ),
      bagDigestItem = BagDigestFile(
        checksum = digest,
        path = BagItemPath(s3Key)
      )
    )

  // todo
  def createArchiveJobWith(
    file: File,
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    bucket: Bucket
  ): ArchiveJob = {
    val bagLocation = BagLocation(
      storageNamespace = bucket.name,
      storagePrefix = "archive",
      storageSpace = randomStorageSpace,
      bagPath = BagPath(bagIdentifier.toString)
    )

    ArchiveJob(
      externalIdentifier = bagIdentifier,
      zipFile = new ZipFile(file),
      bagUploadLocation = bagLocation,
      tagManifestLocation = BagItemPath("tagmanifest-sha256.txt"),
      bagManifestLocations = List(
            BagItemPath("manifest-sha256.txt"),
            BagItemPath("tagmanifest-sha256.txt")
      ),
      config = BagItConfig()
    )
  }
}
