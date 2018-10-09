package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.EntryPath

case class ZipLocation(
  zipFile: ZipFile,
  entryPath: EntryPath
)

object ZipLocation {
  def apply(archiveItemJob: ArchiveItemJob): ZipLocation = {
    ZipLocation(
      archiveItemJob.archiveJob.zipFile,
      archiveItemJob.bagDigestItem.location
    )
  }
}
