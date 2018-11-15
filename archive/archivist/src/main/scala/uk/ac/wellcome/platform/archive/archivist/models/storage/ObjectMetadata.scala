package uk.ac.wellcome.platform.archive.archivist.models.storage

import com.amazonaws.services.s3.model.{ObjectMetadata => S3ObjectMetadata}

case class ObjectMetadata(userMetadata: Map[String, String]) {
  def toS3ObjectMetadata: S3ObjectMetadata = {
    val s3ObjectMetadata = new S3ObjectMetadata()
    for ((k, v) <- userMetadata)
      s3ObjectMetadata.addUserMetadata(k, v)
    s3ObjectMetadata
  }
}
