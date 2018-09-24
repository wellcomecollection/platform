package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.stream.scaladsl.{Flow, StreamConverters}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ZipLocation
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

object ZipFileEntryFlow extends Logging {
  def apply() = {
    Flow[ZipLocation]
      .log("reading input stream")
      .map(ZipFileReader.maybeInputStream).collect{ case Some(inputStream) => inputStream}
      .log("converting inputstream")
      .flatMapConcat { inputStream => sourceFrom(inputStream)
      }
  }

  private def sourceFrom(inputStream: InputStream) =
    StreamConverters.fromInputStream(() => inputStream)
}
