package uk.ac.wellcome.platform.snapshot_generator.flow

import akka.NotUsed
import akka.stream.scaladsl.{Compression, Flow}
import akka.util.ByteString
import com.twitter.inject.Logging

/** Converts instances of String into gzip-compressed ByteString.
  *
  * This is used to prepare the gzip file we uploaded to S3.
  */
object StringToGzipFlow extends Logging {

  def apply(): Flow[String, ByteString, NotUsed] =
    Flow[String]
      .map { _ + "\n" }
      .map { ByteString(_) }
      .via(Compression.gzip)
}
