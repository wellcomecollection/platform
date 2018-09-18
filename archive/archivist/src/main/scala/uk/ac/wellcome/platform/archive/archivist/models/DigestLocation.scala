package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.BagPath
import uk.ac.wellcome.storage.ObjectLocation

case class DigestLocation(path: BagPath, name: String) {
  def toObjectLocation = ObjectLocation(path.value, name)
}

object DigestLocation {
  def create(archiveJob: ArchiveJob) =
    archiveJob.config.digestNames
      .map(digestName => {
        DigestLocation(archiveJob.bagLocation.bagPath, digestName)
      })
}
