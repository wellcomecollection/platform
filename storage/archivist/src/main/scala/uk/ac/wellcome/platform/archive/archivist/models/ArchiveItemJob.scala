package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{NeeeeeeewBagItemLocation, NeeeeeewBagItemPath}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveItemJob(archiveJob: ArchiveJob, bagItemPath: NeeeeeewBagItemPath) {
  def uploadLocation: ObjectLocation = {
    val bagItemLocation = NeeeeeeewBagItemLocation(
      bagLocation = archiveJob.bagLocation,
      bagItemPath = bagItemPath
    )

    bagItemLocation.objectLocation
  }
}
