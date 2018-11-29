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
      archiveJob = createArchiveJob(zipFile, bagIdentifier, bucket),
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
      archiveJob = createArchiveJob(zipFile, bagIdentifier, bucket),
      bagDigestItem = BagItem(digest, EntryPath(s3Key))
    )

  def createArchiveJob(
    zipFile: ZipFile,
    bagIdentifier: ExternalIdentifier,
    bucket: Bucket,
    manifestFiles: List[String] =
      List("manifest-sha256.txt", "tagmanifest-sha256.txt")) = {
    val bagPath = BagPath(s"space/$bagIdentifier")

    val bagLocation = BagLocation(bucket.name, "archive", bagPath)
    ArchiveJob(
      bagIdentifier,
      zipFile,
      bagLocation,
      BagItConfig(),
      manifestFiles.map(BagManifestLocation(_)))
  }
}
