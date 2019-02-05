package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.model.CopyResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}

class S3Copier(s3Client: AmazonS3) extends Logging {

  import com.amazonaws.services.s3.transfer.TransferManagerBuilder

  private val transferManager = TransferManagerBuilder.standard
    .withS3Client(s3Client)
    .build // Fixed thread pool

  def copy(
    src: ObjectLocation,
    dst: ObjectLocation
  )(implicit
    ec: ExecutionContext): Future[CopyResult] = Future {
    debug(s"Copying ${s3Uri(src)} -> ${s3Uri(dst)}")

    val copyTransfer = transferManager.copy(
      src.namespace,
      src.key,
      dst.namespace,
      dst.key
    )

    copyTransfer.waitForCopyResult()
  }

  private def s3Uri(objectLocation: ObjectLocation): String =
    s"s3://${objectLocation.namespace}/${objectLocation.key}"
}
