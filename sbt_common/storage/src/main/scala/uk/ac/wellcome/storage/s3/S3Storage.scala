package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.twitter.inject.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.hashing.MurmurHash3

object S3Storage extends Logging {

  def put(s3Client: AmazonS3, bucketName: String)(
    keyPrefix: String, keySuffix: Option[String] = None)(is: InputStream)(
    implicit ec: ExecutionContext): Future[S3ObjectLocation] = {

    // Currently hiding the stringification, so we can deal with it later if we need to
    val content = scala.io.Source.fromInputStream(is).mkString
    val contentHash = MurmurHash3.stringHash(content, MurmurHash3.stringSeed)

    // Ensure that keyPrefix here is normalised for concatenating with contentHash
    val prefix = keyPrefix
      .stripPrefix("/")
      .stripSuffix("/")

    val suffix = keySuffix
      .map { _.stripPrefix(".") }
      .map { "." + _ }
      .getOrElse("")

    val key = s"$prefix/$contentHash$suffix"

    info(s"Attempt: PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(bucketName, key, content)
    }

    putObject.map { _ =>
      info(s"Success: PUT object to s3://$bucketName/$key")
      S3ObjectLocation(bucketName, key)
    }
  }

  def get(s3Client: AmazonS3, bucketName: String)(key: String)(
    implicit ec: ExecutionContext): Future[InputStream] = {
    info(s"Attempt: GET object from s3://$bucketName/$key")

    val futureInputStream = Future {
      s3Client.getObject(bucketName, key).getObjectContent
    }

    futureInputStream.foreach {
      case _ =>
        info(s"Success: GET object from s3://$bucketName/$key")
    }

    futureInputStream
  }
}
