package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.ObjectLocation

case class ArchiveDigestItemJob(archiveJob: ArchiveJob,
                                bagDigestItem: BagDigestFile) {
  def bagName: BagPath = archiveJob.bagLocation.bagPath

  def uploadLocation: ObjectLocation =
    createUploadLocation(archiveJob.bagLocation, bagDigestItem.path)

  private def createUploadLocation(
                                    bagLocation: BagLocation,
                                    itemLocation: BagFilePath
  ) =
    ObjectLocation(
      namespace = bagLocation.storageNamespace,
      key = List(bagLocation.completeFilepath, itemLocation.value).mkString("/")
    )
}
