package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{
  BagItem,
  BagLocation,
  EntryPath
}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveItemJob(archiveJob: ArchiveJob, bagDigestItem: BagItem) {
  def bagName = archiveJob.bagLocation.bagPath

  def uploadLocation =
    createUploadLocation(archiveJob.bagLocation, bagDigestItem.location)

  private def createUploadLocation(
    bagLocation: BagLocation,
    itemLocation: EntryPath
  ) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagPath.value,
        itemLocation.path
      ).mkString("/")
    )
}
