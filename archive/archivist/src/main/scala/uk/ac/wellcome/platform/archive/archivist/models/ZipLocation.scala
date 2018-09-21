package uk.ac.wellcome.platform.archive.archivist.models

import java.io.InputStream
import java.util.zip.ZipFile

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

case class ZipLocation(
                        zipFile: ZipFile,
                        objectLocation: ObjectLocation
                      ) extends Logging{
  def inputStream: Option[InputStream] = {
    info(s"objectLocation: $objectLocation")
    val name = s"${objectLocation.namespace}/${objectLocation.key}"
    info(s"Getting ZipEntry $name")

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
