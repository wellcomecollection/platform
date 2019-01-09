package uk.ac.wellcome.platform.archive.archivist.generators

import java.io.File
import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.generators.ExternalIdentifierGenerators
import uk.ac.wellcome.platform.archive.common.models
import uk.ac.wellcome.platform.archive.common.models._
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
      itemLocation = BagFilePath(s3Key)
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
      bagDigestItem = models.BagDigestFile(Checksum(digest), BagFilePath(s3Key))
    )

  def createArchiveJobWith(
    file: File,
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    bucket: Bucket
  ): ArchiveJob = {
    val bagPath = BagPath(s"space/$bagIdentifier")

    val bagLocation = BagLocation(bucket.name, "archive", bagPath)
    ArchiveJob(
      externalIdentifier = bagIdentifier,
      zipFile = new ZipFile(file),
      bagLocation = bagLocation,
      config = BagItConfig(),
      bagManifestLocations = List(
        BagManifestLocation("manifest-sha256.txt"),
        BagManifestLocation("tagmanifest-sha256.txt")
      )
    )
  }
}
