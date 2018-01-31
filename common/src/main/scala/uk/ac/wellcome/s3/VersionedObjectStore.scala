package uk.ac.wellcome.s3

import java.security.MessageDigest

import com.amazonaws.services.s3.AmazonS3
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models.{VersionUpdater, Versioned}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.io.Source
import scala.util.hashing.MurmurHash3

class VersionedObjectStore(s3Client: AmazonS3, bucketName: String) {
  def put[T <: Versioned](versionedObject: T)(
    implicit encoder: Encoder[T],
    versionUpdater: VersionUpdater[T]): Future[String] = {

    val newVersion = versionedObject.version + 1
    val newObject =
      versionUpdater.updateVersion(versionedObject, newVersion = newVersion)

    Future.fromTry(JsonUtil.toJson(newObject)).map { content =>
      val contentHash = MurmurHash3.stringHash(content)
      val key =
        s"${newObject.sourceName}/${newObject.sourceId}/${newObject.version}/$contentHash.json"

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
