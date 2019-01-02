package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{BagDigestFile, BagFilePath, BagLocation}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveDigestItemJob(archiveJob: ArchiveJob,
                                bagDigestItem: BagDigestFile) {
  def bagName = archiveJob.bagLocation.bagPath

  def uploadLocation =
    createUploadLocation(archiveJob.bagLocation, bagDigestItem.path)

  private def createUploadLocation(
    bagLocation: BagLocation,
    itemLocation: BagFilePath
  ) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagPath.value,
        itemLocation.value
      ).mkString("/")
    )
}
