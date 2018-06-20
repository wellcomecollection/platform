package uk.ac.wellcome.storage.s3

import java.io.{ByteArrayInputStream, InputStream}

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.util.IOUtils
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.{ObjectLocation, StorageBackend}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

class S3StorageBackend @Inject()(s3Client: AmazonS3)(
  implicit ec: ExecutionContext)
    extends StorageBackend
    with Logging {

  private def generateMetadata(
    userMetadata: Map[String, String],
    contentLength: Int
  ): ObjectMetadata = {
    val objectMetadata = new ObjectMetadata()
    objectMetadata.setUserMetadata(userMetadata.asJava)
    objectMetadata.setContentLength(contentLength)
    objectMetadata
  }

  def put(location: ObjectLocation,
          input: InputStream,
          metadata: Map[String, String]): Future[Unit] = {

    // Yes, it's moderately daft that we get an InputStream which we
    // immediately load into a ByteArray, then turn it into a different
    // InputStream for the upload to S3.
    //
    // Doing it this way allows S3 to know the length of the data before
    // we upload it.  Otherwise we get the following warning:
    //
    //      No content length specified for stream data. Stream
    //      contents will be buffered in memory and could result
    //      in out of memory errors.
    //
    // When we have bigger strings than fit in memory, we'll need to
    // revisit this code anyway -- it'll be time for multipart uploads.
    //
    val bytes = IOUtils.toByteArray(input)
    val byteArrayInputStream = new ByteArrayInputStream(bytes)

    val generatedMetadata = generateMetadata(
      userMetadata = metadata,
      contentLength = bytes.length
    )

    val bucketName = location.namespace
    val key = location.key

    val putObjectRequest = new PutObjectRequest(
      bucketName,
      key,
      byteArrayInputStream,
      generatedMetadata
    )

    debug(s"Attempt: PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(putObjectRequest)
    }

    putObject.map { _ =>
      debug(s"Success: PUT object to s3://$bucketName/$key")
      ObjectLocation(bucketName, key)
    }
  }

  def get(location: ObjectLocation): Future[InputStream] = {
    val bucketName = location.namespace
    val key = location.key

    debug(s"Attempt: GET object from s3://$bucketName/$key")

    val futureInputStream = Future {
      s3Client.getObject(bucketName, key).getObjectContent
    }

    futureInputStream.foreach {
      case _ =>
        debug(s"Success: GET object from s3://$bucketName/$key")
    }

    futureInputStream
  }
}
