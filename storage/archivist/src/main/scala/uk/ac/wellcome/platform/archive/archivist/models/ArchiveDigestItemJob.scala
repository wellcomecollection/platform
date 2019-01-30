package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{BagDigestFile, BagPath}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveDigestItemJob(archiveJob: ArchiveJob,
                                bagDigestItem: BagDigestFile) {
  def bagName: BagPath = archiveJob.bagLocation.bagPath

  def uploadLocation: ObjectLocation =
    archiveJob.bagLocation.getFileObjectLocation(
      filename = bagDigestItem.path.value
    )
}
