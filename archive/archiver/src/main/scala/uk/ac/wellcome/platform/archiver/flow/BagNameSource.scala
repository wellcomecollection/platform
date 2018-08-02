package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.Source
import grizzled.slf4j.Logging

object BagNameSource extends Logging {
  def apply(zipFile: ZipFile): Source[String, NotUsed] = {
    val entries = zipFile.entries()

    val tld = Stream
      .continually(entries.nextElement)
      .map(_.getName.split("/"))
      .flatMap(_.headOption)
      .filterNot(_.equals("."))
      .takeWhile(_ => entries.hasMoreElements)
      .toSet

    Source.fromIterator(() => tld.toIterator)
  }
}