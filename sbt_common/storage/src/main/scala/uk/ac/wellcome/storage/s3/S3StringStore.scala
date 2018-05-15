package uk.ac.wellcome.storage.s3

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source


class S3StringStore @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config
) extends Logging
    with S3ObjectStore[String]{
  def put(content: String, keyPrefix: String): Future[S3ObjectLocation] = {
    val is = new ByteArrayInputStream(content.getBytes)

    S3Storage.put(s3Client, s3Config.bucketName)(keyPrefix)(is)
  }

  def get(s3ObjectLocation: S3ObjectLocation): Future[String] = {
    val bucket = s3ObjectLocation.bucket
    val key = s3ObjectLocation.key

    if (bucket != s3Config.bucketName) {
      debug(
        s"Bucket name in S3ObjectLocation ($bucket) does not match configured bucket (${s3Config.bucketName})")
    }

    S3Storage.get(s3Client, bucket)(key).map (is =>
      Source.fromInputStream(is).mkString
    )
  }
}