package uk.ac.wellcome.platform.archive.archivist.zipfile

import java.io.InputStream

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ZipLocation

object ZipFileReader extends Logging {
  def maybeInputStream(zipLocation: ZipLocation): Option[InputStream] = {
    val zipFile = zipLocation.zipFile
    debug(s"Getting ZipEntry ${zipLocation.entryPath}")

    val maybeInputStream = for {
      zipEntry <- Option(zipFile.getEntry(zipLocation.entryPath.path))
      zipStream <- Option(zipFile.getInputStream(zipEntry))
    } yield zipStream

    debug(s"MaybeInputStream: $maybeInputStream")
    maybeInputStream
  }
}
