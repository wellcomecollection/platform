package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.stream.scaladsl.{Flow, StreamConverters}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ZipLocation

object ZipFileEntryFlow extends Logging {
  def apply() = {
    Flow[ZipLocation]
      .map(_.inputStream)
      .flatMapConcat {
        case Some(inputStream) => sourceFrom(inputStream)
        //TODO: THIS IS WRONG
        case None => throw new RuntimeException("foof")
      }
  }

  private def sourceFrom(inputStream: InputStream) =
    StreamConverters.fromInputStream(() => inputStream)
}
