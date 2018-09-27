package uk.ac.wellcome.platform.archive.archivist.zipfile

import java.io.InputStream

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ZipLocation

object ZipFileReader extends Logging {
  def maybeInputStream(zipLocation: ZipLocation): Option[InputStream] = {
    val objectLocation = zipLocation.objectLocation
    val zipFile = zipLocation.zipFile
    info(s"objectLocation: $objectLocation")
    val name = List(objectLocation.namespace,objectLocation.key).filterNot(_.isEmpty).mkString("/")
    info(s"Getting ZipEntry $name")

    val maybeInputStream = for {
      zipEntry <- Option(zipFile.getEntry(name))
      zipStream <- Option(zipFile.getInputStream(zipEntry))
    } yield zipStream

    info(s"MaybeInputStream: $maybeInputStream")
    maybeInputStream
  }
}
