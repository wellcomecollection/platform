package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.{Compression, Flow}
import akka.util.ByteString
import com.twitter.inject.Logging

import scala.concurrent.ExecutionContext

/** Converts instances of String into gzip-compressed ByteString.
  *
  * This is used to prepare the gzip file we uploaded to S3.
  */
object StringToGzipFlow extends Logging {

  def apply()(implicit executionContext: ExecutionContext): Flow[String, ByteString, NotUsed] =
    Flow[String]
      .map { ByteString(_) }
      .via(Compression.gzip)
}
