package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemLocation,
  BagItemPath
}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveItemJob(archiveJob: ArchiveJob, bagItemPath: BagItemPath) {
  def uploadLocation: ObjectLocation = {
    val bagItemLocation = BagItemLocation(
      bagLocation = archiveJob.bagUploadLocation,
      bagItemPath = bagItemPath
    )

    bagItemLocation.objectLocation
  }
}
