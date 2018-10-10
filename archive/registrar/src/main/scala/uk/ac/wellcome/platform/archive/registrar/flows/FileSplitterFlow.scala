package uk.ac.wellcome.platform.archive.registrar.flows

import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString

object FileSplitterFlow {
  val framingDelimiter = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  )

  def apply(delimiter: String) =
    Flow[ByteString]
      .via(framingDelimiter)
      .map(_.utf8String)
      .filter(_.nonEmpty)
      .map(createTuple(_, delimiter))

  private def createTuple(fileChunk: String, delimiter: String) = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(key: String, value: String) => (key, value)
      case _ =>
        throw MalformedLineException(
          splitChunk.mkString(delimiter)
        )
    }
  }
}

case class MalformedLineException(line: String)
    extends RuntimeException(s"Malformed bag digest line: $line")
