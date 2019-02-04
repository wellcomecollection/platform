package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{BagFilePath, BagPath, BagLocation}
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveItemJob(archiveJob: ArchiveJob, itemLocation: BagFilePath) {
  def bagName: BagPath = archiveJob.bagLocation.bagPath

  def uploadLocation: ObjectLocation =
    createUploadLocation(archiveJob.bagLocation, itemLocation)

  private def createUploadLocation(
                                    bagLocation: BagLocation,
                                    itemLocation: BagFilePath
  ) =
    ObjectLocation(
      namespace = bagLocation.storageNamespace,
      key = List(bagLocation.completeFilepath, itemLocation.value).mkString("/")
    )
}
