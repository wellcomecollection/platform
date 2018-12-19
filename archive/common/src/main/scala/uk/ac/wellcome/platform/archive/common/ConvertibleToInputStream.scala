package uk.ac.wellcome.platform.archive.common

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.Try

object ConvertibleToInputStream {

  implicit class ConvertibleToInputStreamOps[T](t: T) {
    def toInputStream(implicit toInputStream: ToInputStream[T],
                      s3Client: AmazonS3): Try[InputStream] = {
      toInputStream.apply(t)
    }
  }

  implicit object ConvertibleToInputStreamObjectLocation
      extends ToInputStream[ObjectLocation] {

    def apply(t: ObjectLocation)(
      implicit s3Client: AmazonS3): Try[InputStream] =
      Try(
        s3Client.getObject(t.namespace, t.key)
      ).map(
        response => response.getObjectContent
      )
  }
}

trait ToInputStream[T] {
  def apply(t: T)(implicit s3Client: AmazonS3): Try[InputStream]
}
