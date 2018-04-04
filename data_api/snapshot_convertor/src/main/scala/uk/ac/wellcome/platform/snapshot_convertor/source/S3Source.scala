package uk.ac.wellcome.platform.snapshot_convertor.source

import java.io.InputStream

import akka.stream.scaladsl.{Compression, Framing, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import akka.stream.scaladsl.StreamConverters.fromInputStream

/** Given an S3 bucket and key that point to a gzip-compressed file, this
  * source emits the (uncompressed) lines from that file, one line at a time.
  */
object S3Source {
  def apply(s3client: AmazonS3,
            bucketName: String,
            key: String): Source[String, Any] = {

    val s3inputStream: InputStream = s3client
      .getObject(bucketName, key)
      .getObjectContent()

    val s3source: Source[ByteString, Any] = fromInputStream(
      in = {() => s3inputStream},
      chunkSize = 1024
    )

    s3source
      .via(Compression.gunzip(maxBytesPerChunk = 1024))
      .via(Framing
        .delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true))
      .map { _.utf8String }
  }
}
