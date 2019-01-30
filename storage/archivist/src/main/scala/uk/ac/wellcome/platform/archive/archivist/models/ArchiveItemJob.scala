package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{BagFilePath, BagPath}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveItemJob(archiveJob: ArchiveJob, itemLocation: BagFilePath) {
  def bagName: BagPath = archiveJob.bagLocation.bagPath

  def uploadLocation: ObjectLocation =
    archiveJob.bagLocation.getFileObjectLocation(
      filename = itemLocation.value
    )
}
