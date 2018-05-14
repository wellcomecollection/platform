package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source
import scala.util.hashing.MurmurHash3

class S3StringStore @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config
) extends Logging
    with S3Store[String] {
  def put(content: String, keyPrefix: String): Future[S3ObjectLocation] =
    S3StringStore.put(s3Client, s3Config.bucketName)(keyPrefix)(content)

  def get(s3ObjectLocation: S3ObjectLocation): Future[String] = {
    val bucket = s3ObjectLocation.bucket
    val key = s3ObjectLocation.key

    if (bucket != s3Config.bucketName) {
      debug(
        s"Bucket name in S3ObjectLocation ($bucket) does not match configured bucket (${s3Config.bucketName})")
    }

    S3StringStore.get(s3Client, bucket)(key)
  }
}

object S3StringStore extends Logging {
  def put(s3Client: AmazonS3, bucketName: String)(keyPrefix: String)(
    content: String): Future[S3ObjectLocation] = {
    val contentHash = MurmurHash3.stringHash(content, MurmurHash3.stringSeed)

    // Ensure that keyPrefix here is normalised for concatenating with contentHash
    val prefix = keyPrefix
      .stripPrefix("/")
      .stripSuffix("/")

    val key = s"$prefix/$contentHash.json"

    info(s"Attempting to PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(bucketName, key, content)
    }

    putObject.map { _ =>
      info(s"Successfully PUT object to s3://$bucketName/$key")
      S3ObjectLocation(bucketName, key)
    }
  }

  def get(s3Client: AmazonS3, bucketName: String)(
    key: String): Future[String] = {
    info(s"Attempting to GET object from s3://$bucketName/$key")

    val getObject = Future {
      val s3Object = s3Client.getObject(bucketName, key)
      Source.fromInputStream(s3Object.getObjectContent).mkString
    }

    getObject.map { s3ObjectContent =>
      info(s"Successful GET object from s3://$bucketName/$key")
      s3ObjectContent
    }
  }
}
