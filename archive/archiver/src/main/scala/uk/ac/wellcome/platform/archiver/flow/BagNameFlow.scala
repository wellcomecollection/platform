package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging

object BagNameFlow extends Logging {
  def apply(): Flow[ZipFile, String, NotUsed] = {

    Flow[ZipFile].map(_.entries).mapConcat(entries => {
      Stream
        .continually(entries.nextElement)
        .map(_.getName.split("/"))
        .flatMap(_.headOption)
        .filterNot(_.equals("."))
        .takeWhile(_ => entries.hasMoreElements)
        .toSet
    })

  }
}