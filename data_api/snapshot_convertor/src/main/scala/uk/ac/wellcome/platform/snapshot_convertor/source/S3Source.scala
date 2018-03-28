package uk.ac.wellcome.platform.snapshot_convertor.source

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Compression, Framing, Source}
import akka.util.ByteString

/** Given an S3 bucket and key that point to a gzip-compressed file, this
  * source emits the (uncompressed) lines from that file, one line at a time.
  */
object S3Source {
  def apply(s3client: S3Client, bucketName: String, key: String): Source[String, Any] = {
    val (s3download: Source[ByteString, _], _) = s3client.download(bucket = bucketName, key = key)

    s3download
      .via(Compression.gunzip())
      .via(Framing
        .delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true)
      )
      .map { _.utf8String }
  }
}
