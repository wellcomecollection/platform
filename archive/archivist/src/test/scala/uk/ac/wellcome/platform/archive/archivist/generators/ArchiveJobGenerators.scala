package uk.ac.wellcome.platform.archive.archivist.generators

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait ArchiveJobGenerators {


  def createArchiveItemJob(zipFile: ZipFile,
                           bucket: S3.Bucket,
                           bagIdentifier: ExternalIdentifier,
                           s3Key: String) = {
    ArchiveItemJob(
        archiveJob = createArchiveJob(zipFile, bagIdentifier, bucket),
        itemLocation = EntryPath(s3Key))
  }

  def createArchiveDigestItemJob(zipFile: ZipFile,
                                 bucket: S3.Bucket,
                                 digest: String,
                                 bagIdentifier: ExternalIdentifier,
                                 s3Key: String) = {
      ArchiveDigestItemJob(
        archiveJob = createArchiveJob(zipFile, bagIdentifier, bucket),
        bagDigestItem = BagItem(digest, EntryPath(s3Key)))
  }

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
