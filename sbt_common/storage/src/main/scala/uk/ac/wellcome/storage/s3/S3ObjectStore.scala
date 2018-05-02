package uk.ac.wellcome.storage.s3

import java.net.URI

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source
import scala.util.hashing.MurmurHash3

class S3ObjectStore[T] @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) extends Logging {
  def put(sourcedObject: T)(
    implicit encoder: Encoder[T]): Future[S3ObjectLocation] = {
    val keyPrefix = keyPrefixGenerator.generate(sourcedObject)

    S3ObjectStore.put[T](s3Client, s3Config.bucketName)(keyPrefix)(
      sourcedObject)
  }

  def get(s3ObjectLocation: S3ObjectLocation)(
    implicit decoder: Decoder[T]): Future[T] = {
    val bucket = s3ObjectLocation.bucket
    val key = s3ObjectLocation.key

    if (bucket != s3Config.bucketName) {
      debug(
        s"Bucket name in S3ObjectLocation ($bucket) does not match configured bucket (${s3Config.bucketName})")
    }

    S3ObjectStore.get[T](s3Client, bucket)(key)
  }
}

object S3ObjectStore extends Logging {
  def put[T](s3Client: AmazonS3, bucketName: String)(keyPrefix: String)(
    sourcedObject: T)(implicit encoder: Encoder[T]): Future[S3ObjectLocation] =
    Future.fromTry(JsonUtil.toJson(sourcedObject)).map { content =>
      val contentHash = MurmurHash3.stringHash(content, MurmurHash3.stringSeed)

      // Ensure that keyPrefix here is normalised for concatenating with contentHash
      val prefix = keyPrefix
        .stripPrefix("/")
        .stripSuffix("/")

      val key = s"$prefix/$contentHash.json"

      info(s"Attempting to PUT object to s3://$bucketName/$key")
      s3Client.putObject(bucketName, key, content)
      info(s"Successfully PUT object to s3://$bucketName/$key")

      S3ObjectLocation(bucketName, key)
    }

  def get[T](s3Client: AmazonS3, bucketName: String)(key: String)(
    implicit decoder: Decoder[T]): Future[T] = {

    info(s"Attempting to GET object from s3://$bucketName/$key")

    val getObject = Future {
      val s3Object = s3Client.getObject(bucketName, key)
      Source.fromInputStream(s3Object.getObjectContent).mkString
    }

    getObject.flatMap(s3ObjectContent => {
      info(s"Successful GET object from s3://$bucketName/$key")

      Future.fromTry(JsonUtil.fromJson[T](s3ObjectContent))
    })
  }
}
