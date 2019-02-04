package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.bagit.BagItemPath

case class ZipLocation(
  zipFile: ZipFile,
  bagItemPath: BagItemPath
)

object ZipLocation {
  def apply(archiveItemJob: ArchiveDigestItemJob): ZipLocation =
    ZipLocation(
      archiveItemJob.archiveJob.zipFile,
      archiveItemJob.bagDigestItem.path
    )
}
