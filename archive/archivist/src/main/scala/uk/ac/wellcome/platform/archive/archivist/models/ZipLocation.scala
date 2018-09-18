package uk.ac.wellcome.platform.archive.archivist.models

import java.io.InputStream
import java.util.zip.ZipFile

import uk.ac.wellcome.storage.ObjectLocation

case class ZipLocation(
                        zipFile: ZipFile,
                        objectLocation: ObjectLocation
                      ) {
  def inputStream: Option[InputStream] = {
    val name = s"${objectLocation.namespace}/${objectLocation.key}"

    val maybeInputStream = for {
      zipEntry <- Option(zipFile.getEntry(name))
      zipStream <- Option(zipFile.getInputStream(zipEntry))
    } yield zipStream

    maybeInputStream
  }
}

object ZipLocation {
  def apply(archiveItemJob: ArchiveItemJob): ZipLocation = {
    ZipLocation(
      archiveItemJob.archiveJob.zipFile,
      archiveItemJob.bagDigestItem.location
    )
  }
}
