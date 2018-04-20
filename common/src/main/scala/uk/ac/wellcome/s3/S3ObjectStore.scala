package uk.ac.wellcome.s3

import java.net.URI

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.s3.S3Uri
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source
import scala.util.hashing.MurmurHash3

// '-T' means KeyPrefixGenerator is contravariant for type T
//
// This means that if S is a subclass of T, then KeyPrefixGenerator[T]
// is a subclass of KeyPrefixGenerator[S] (the class hierarchy is reversed).
//
// For example,
//
//      Sourced < SierraTransformable
//
// but
//
//      KeyPrefixGenerator[SierraTransformable] < KeyPrefixGenerator[Sourced]
//
// In S3ObjectStore, we need a KeyPrefixGenerator[T], where T is usually some
// subclass of Sourced (this is the way we use it, not a strict requirement).
// This contravariance allows us to define a single KeyPrefixGenerator[Sourced]
// and use it on all instances, even though T is some subclass of Sourced.

trait KeyPrefixGenerator[-T] {
  def generate(obj: T): String
}

// To spread objects evenly in our S3 bucket, we take the last two
// characters of the ID and reverse them.  This ensures that:
//
//  1.  It's easy for a person to find the S3 data corresponding to
//      a given source ID.
//
//  2.  Adjacent objects are stored in shards that are far apart,
//      e.g. b0001 and b0002 are separated by nine shards.

class SourcedKeyPrefixGenerator @Inject() extends KeyPrefixGenerator[Sourced] {
  override def generate(obj: Sourced): String = {
    val s3Shard = obj.sourceId.reverse.slice(0, 2)

    s"${obj.sourceName}/${s3Shard}/${obj.sourceId}"
  }
}

class S3ObjectStore[T] @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) extends Logging {
  def put(sourcedObject: T)(implicit encoder: Encoder[T]): Future[URI] = {
    val keyPrefix = keyPrefixGenerator.generate(sourcedObject)
    S3ObjectStore.put[T](s3Client, s3Config.bucketName)(keyPrefix)(
      sourcedObject)
  }

  def get(uri: URI)(implicit decoder: Decoder[T]): Future[T] = {
    uri match {
      case S3Uri(bucket, key) => {

        if (bucket != s3Config.bucketName) {
          debug(
            s"Bucket name in URI ($bucket) does not match configured bucket (${s3Config.bucketName})")
        }

        S3ObjectStore.get[T](s3Client, bucket)(key)
      }
      case _ =>
        Future.failed(
          new RuntimeException(
            s"Invalid URI scheme when trying to get from s3 $uri"))
    }
  }
}

object S3ObjectStore extends Logging {
  def put[T](s3Client: AmazonS3, bucketName: String)(keyPrefix: String)(
    sourcedObject: T)(implicit encoder: Encoder[T]): Future[URI] =
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

      S3Uri(bucketName, key)
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
