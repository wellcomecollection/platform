package uk.ac.wellcome.platform.archive.archivist.zipfile

import java.io.InputStream

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ZipLocation

object ZipFileReader extends Logging {

  /** Returns an InputStream for a file inside a ZIP file.
    *
    * If something goes wrong (for example, the file doesn't exist),
    * it returns None rather than throwing an exception.
    *
    */
  def maybeInputStream(zipLocation: ZipLocation): Option[InputStream] = {
    val zipFile = zipLocation.zipFile
    debug(s"Getting ZipEntry ${zipLocation.bagItemPath}")

    val maybeInputStream = for {
      zipEntry <- Option(zipFile.getEntry(zipLocation.bagItemPath.toString))
      zipStream <- Option(zipFile.getInputStream(zipEntry))
    } yield zipStream

    debug(s"MaybeInputStream: $maybeInputStream")
    maybeInputStream
  }
}
