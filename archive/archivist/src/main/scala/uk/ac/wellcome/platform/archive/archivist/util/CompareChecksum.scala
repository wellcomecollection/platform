package uk.ac.wellcome.platform.archive.archivist.util

import akka.util.ByteString
import grizzled.slf4j.Logging

import scala.util.Try

trait CompareChecksum extends Logging {
  def compare[T](checksum: String): PartialFunction[(T, ByteString), Try[T]] = {
    case (result, byteChecksum: ByteString) => Try {

      val calculatedChecksum = byteChecksum
        .map(0xFF & _)
        .map("%02x".format(_))
        .foldLeft("") {
          _ + _
        }
        .mkString

      if (calculatedChecksum != checksum) {
        throw new RuntimeException(
          s"Bad checksum! ($calculatedChecksum != $checksum"
        )
      } else {
        debug(s"Checksum match! ($calculatedChecksum != $checksum")
      }

      result
    }
  }
}
