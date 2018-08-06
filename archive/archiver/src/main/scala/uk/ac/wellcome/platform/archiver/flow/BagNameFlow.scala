package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging

object BagNameFlow extends Logging {
  def apply(): Flow[ZipFile, String, NotUsed] = {

    Flow[ZipFile]
      .map(_.entries)
      .log("zip entry")
      .mapConcat(entries => {
        Stream
          .continually(entries.nextElement)
          .map(_.getName.split("/"))
          .filter(_.length > 1)
          .flatMap(_.headOption)
          .takeWhile(_ => entries.hasMoreElements)
          .toSet
          .filterNot(_.startsWith("_"))
      })
      .log("bag name")
  }
}
