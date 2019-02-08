package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagDigestFile,
  BagItemLocation
}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveDigestItemJob(archiveJob: ArchiveJob,
                                bagDigestItem: BagDigestFile) {
  def uploadLocation: ObjectLocation = {
    val bagItemLocation = BagItemLocation(
      bagLocation = archiveJob.bagUploadLocation,
      bagItemPath = bagDigestItem.path
    )

    bagItemLocation.objectLocation
  }
}
