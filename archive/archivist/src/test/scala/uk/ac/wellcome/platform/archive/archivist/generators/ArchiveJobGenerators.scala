package uk.ac.wellcome.platform.archive.archivist.generators

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.generators.ExternalIdentifierGenerators
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait ArchiveJobGenerators extends ExternalIdentifierGenerators {

  def createArchiveItemJobWith(
    zipFile: ZipFile,
    bucket: S3.Bucket,
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    s3Key: String
  ): ArchiveItemJob =
    ArchiveItemJob(
      archiveJob = createArchiveJobWith(
        zipFile = zipFile,
        bagIdentifier = bagIdentifier,
        bucket = bucket
      ),
      itemLocation = EntryPath(s3Key)
    )

  def createArchiveDigestItemJobWith(
    zipFile: ZipFile,
    bucket: S3.Bucket,
    digest: String,
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    s3Key: String
  ): ArchiveDigestItemJob =
    ArchiveDigestItemJob(
      archiveJob = createArchiveJobWith(
        zipFile = zipFile,
        bagIdentifier = bagIdentifier,
        bucket = bucket
      ),
      bagDigestItem = BagItem(digest, EntryPath(s3Key))
    )

  def createArchiveJobWith(
    zipFile: ZipFile,
    bagIdentifier: ExternalIdentifier = createExternalIdentifier,
    bucket: Bucket
  ): ArchiveJob = {
    val bagPath = BagPath(s"space/$bagIdentifier")

    val bagLocation = BagLocation(bucket.name, "archive", bagPath)
    ArchiveJob(
      externalIdentifier = bagIdentifier,
      zipFile = zipFile,
      bagLocation = bagLocation,
      config = BagItConfig(),
      bagManifestLocations = List(
        BagManifestLocation("manifest-sha256.txt"),
        BagManifestLocation("tagmanifest-sha256.txt")
      )
    )
  }
}
