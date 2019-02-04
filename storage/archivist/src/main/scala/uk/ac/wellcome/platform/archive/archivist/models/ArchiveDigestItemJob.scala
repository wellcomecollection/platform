package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveDigestItemJob(archiveJob: ArchiveJob,
                                bagDigestItem: BagDigestFile) {
  def uploadLocation: ObjectLocation = {
    val bagItemLocation = NeeeeeeewBagItemLocation(
      bagLocation = archiveJob.bagLocation,
      bagItemPath = bagDigestItem.path
    )

    bagItemLocation.objectLocation
  }
}
