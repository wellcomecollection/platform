package uk.ac.wellcome.s3

import java.security.MessageDigest

import com.amazonaws.services.s3.AmazonS3
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models.Versioned
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source
import scala.util.hashing.MurmurHash3

class VersionedObjectStore(s3Client: AmazonS3, bucketName: String) {
  def put[T <: Versioned](versionedObject: T)(
    implicit encoder: Encoder[T]): Future[String] = {
    Future.fromTry(JsonUtil.toJson(versionedObject)).map { content =>
      val contentHash = MurmurHash3.stringHash(content, MurmurHash3.stringSeed)
      val key =
        s"${versionedObject.sourceName}/${versionedObject.sourceId}/${versionedObject.version}/$contentHash.json"

      s3Client.putObject(bucketName, key, content)

      key
    }
  }

  def get[T <: Versioned](key: String)(
    implicit decoder: Decoder[T]): Future[T] = {
    val getObject = Future {
      val s3Object = s3Client.getObject(bucketName, key)
      Source.fromInputStream(s3Object.getObjectContent).mkString
    }

    getObject.flatMap(s3ObjectContent =>
      Future.fromTry(JsonUtil.fromJson[T](s3ObjectContent)))
  }
}
