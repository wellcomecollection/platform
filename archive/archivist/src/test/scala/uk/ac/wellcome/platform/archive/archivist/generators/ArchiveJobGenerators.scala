package uk.ac.wellcome.platform.archive.archivist.generators

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, BagItConfig, BagManifestLocation}
import uk.ac.wellcome.platform.archive.common.models.{BagItem, BagLocation, BagPath}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3

trait ArchiveJobGenerators {
  def createArchiveJob: ArchiveJob = {
  ???
  }

  def createArchiveItemJob(zipFile: ZipFile, bucket: S3.Bucket, digest: String, bagName: BagPath, s3Key: String, bagDigestItemLocationNamespace: String) = {
    val bagLocation = BagLocation(bucket.name, "archive", bagName)
    val archiveJob = ArchiveJob(zipFile, bagLocation, BagItConfig(), List(BagManifestLocation(bagName, "manifest-sha256.txt")))
    val bagDigestItem = BagItem(digest, ObjectLocation(bagDigestItemLocationNamespace, s3Key))
    val archiveItemJob = ArchiveItemJob(archiveJob = archiveJob, bagDigestItem = bagDigestItem)
    archiveItemJob
  }
}
