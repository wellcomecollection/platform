package uk.ac.wellcome.s3

import java.security.MessageDigest

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source
import scala.util.hashing.MurmurHash3

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
  @Flag("aws.s3.bucketName") bucketName: String,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) extends Logging {
  def put(sourcedObject: T)(implicit encoder: Encoder[T]): Future[String] = {

    val keyPrefix = keyPrefixGenerator.generate(sourcedObject)
    S3ObjectStore.put[T](s3Client, bucketName)(keyPrefix)(sourcedObject)
  }

  def get(key: String)(implicit decoder: Decoder[T]): Future[T] = {
    S3ObjectStore.get[T](s3Client, bucketName)(key)
  }
}

object S3ObjectStore extends Logging {
  def put[T](s3Client: AmazonS3, bucketName: String)(keyPrefix: String)(sourcedObject: T)(
    implicit encoder: Encoder[T]): Future[String] =  Future.fromTry(JsonUtil.toJson(sourcedObject)).map { content =>
    val contentHash = MurmurHash3.stringHash(content, MurmurHash3.stringSeed)

    val prefixCleaningRegex = "^/|/$".r

    val prefix = prefixCleaningRegex.replaceAllIn(keyPrefix, "")

    val key = s"$prefix/$contentHash.json"

    info(s"Attempting to PUT object to s3://$bucketName/$key")
    s3Client.putObject(bucketName, key, content)
    info(s"Successfully PUT object to s3://$bucketName/$key")

    key
  }

  def get[T](s3Client: AmazonS3, bucketName: String)(key: String)(implicit decoder: Decoder[T]): Future[T] = {

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

