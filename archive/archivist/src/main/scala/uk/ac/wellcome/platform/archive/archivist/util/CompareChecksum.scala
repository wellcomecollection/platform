package uk.ac.wellcome.platform.archive.archivist.util

import akka.util.ByteString
import grizzled.slf4j.Logging

trait CompareChecksum extends Logging {
  def compare[T](checksum: String)(byteChecksum: ByteString): Boolean = {
      val calculatedChecksum = byteChecksum
        .map(0xFF & _)
        .map("%02x".format(_))
        .foldLeft("") {
          _ + _
        }
        .mkString

      val isSameChecksum= if (calculatedChecksum != checksum) {
        warn(
          s"Bad checksum! ($calculatedChecksum != $checksum"
        )
        false
      } else {
        debug(s"Checksum match! ($calculatedChecksum != $checksum")
        true
      }

      isSameChecksum
  }
}
