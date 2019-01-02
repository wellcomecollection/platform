package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.BagFilePath

case class ZipLocation(
                        zipFile: ZipFile,
                        bagFilePath: BagFilePath
)

object ZipLocation {
  def apply(archiveItemJob: ArchiveDigestItemJob): ZipLocation = {
    ZipLocation(
      archiveItemJob.archiveJob.zipFile,
      archiveItemJob.bagDigestItem.path
    )
  }
}
