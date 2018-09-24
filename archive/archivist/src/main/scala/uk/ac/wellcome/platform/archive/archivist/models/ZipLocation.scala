package uk.ac.wellcome.platform.archive.archivist.models

import java.io.InputStream
import java.util.zip.ZipFile

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

case class ZipLocation(
                        zipFile: ZipFile,
                        objectLocation: ObjectLocation
                      )

object ZipLocation {
  def apply(archiveItemJob: ArchiveItemJob): ZipLocation = {
    ZipLocation(
      archiveItemJob.archiveJob.zipFile,
      archiveItemJob.bagDigestItem.location
    )
  }
}
