package uk.ac.wellcome.storage.s3

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging

import uk.ac.wellcome.storage.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source


class S3StringStore @Inject()(
  s3Client: AmazonS3,
) extends Logging
    with S3ObjectStore[String]{
  def put(bucket: String)(content: String, keyPrefix: String): Future[S3ObjectLocation] = {
    val is = new ByteArrayInputStream(content.getBytes)

    S3Storage.put(s3Client, bucket)(keyPrefix)(is)
  }

  def get(s3ObjectLocation: S3ObjectLocation): Future[String] = {
    val bucket = s3ObjectLocation.bucket
    val key = s3ObjectLocation.key

    S3Storage.get(s3Client, bucket)(key).map (is =>
      Source.fromInputStream(is).mkString
    )
  }
}