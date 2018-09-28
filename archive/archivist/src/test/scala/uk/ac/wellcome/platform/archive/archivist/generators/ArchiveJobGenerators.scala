package uk.ac.wellcome.platform.archive.archivist.generators

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait ArchiveJobGenerators {

  def createArchiveItemJob(zipFile: ZipFile,
                           bucket: S3.Bucket,
                           digest: String,
                           bagIdentifier: String,
                           s3Key: String) = {
    val archiveJob = createArchiveJob(zipFile, bagIdentifier, bucket)
    val bagDigestItem =
      BagItem(digest, EntryPath(s3Key))
    val archiveItemJob =
      ArchiveItemJob(archiveJob = archiveJob, bagDigestItem = bagDigestItem)
    archiveItemJob
  }

  def createArchiveJob(
    zipFile: ZipFile,
    bagIdentifier: String,
    bucket: Bucket,
    manifestFiles: List[String] =
      List("manifest-sha256.txt", "tagmanifest-sha256.txt")) = {
    val bagPath = BagPath(s"$DigitisedStorageType/$bagIdentifier")
        val bagLocation = BagLocation(bucket.name, "archive", bagPath)
    ArchiveJob(
      zipFile,
      bagLocation,
      BagItConfig(),
      manifestFiles.map(BagManifestLocation(_)))
  }
}
