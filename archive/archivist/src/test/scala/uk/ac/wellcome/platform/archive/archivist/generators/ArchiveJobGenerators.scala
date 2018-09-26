package uk.ac.wellcome.platform.archive.archivist.generators

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob,
  BagItConfig,
  BagManifestLocation
}
import uk.ac.wellcome.platform.archive.common.models.{
  BagItem,
  BagLocation,
  BagPath
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait ArchiveJobGenerators {

  def createArchiveItemJob(zipFile: ZipFile,
                           bucket: S3.Bucket,
                           digest: String,
                           bagName: BagPath,
                           s3Key: String,
                           bagDigestItemLocationNamespace: String) = {
    val archiveJob = createArchiveJob(zipFile, bagName, bucket)
    val bagDigestItem =
      BagItem(digest, ObjectLocation(bagDigestItemLocationNamespace, s3Key))
    val archiveItemJob =
      ArchiveItemJob(archiveJob = archiveJob, bagDigestItem = bagDigestItem)
    archiveItemJob
  }

  def createArchiveJob(
    zipFile: ZipFile,
    bagName: BagPath,
    bucket: Bucket,
    manifestFiles: List[String] =
      List("manifest-sha256.txt", "tagmanifest-sha256.txt")) = {
    val bagLocation = BagLocation(bucket.name, "archive", bagName)
    ArchiveJob(
      zipFile,
      bagLocation,
      BagItConfig(),
      manifestFiles.map(BagManifestLocation(bagName, _)))
  }
}
