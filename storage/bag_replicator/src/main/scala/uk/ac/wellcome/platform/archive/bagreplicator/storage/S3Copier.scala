package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.model.CopyResult
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

class S3Copier(implicit s3Client: AmazonS3) extends Logging {

  import com.amazonaws.services.s3.transfer.TransferManagerBuilder

  private val transferManager = TransferManagerBuilder.standard
    .withS3Client(s3Client)
    .build // Fixed thread pool

  def copy(
    sourceNamespace: String,
    sourceItemKey: String,
    destinationNamespace: String,
    destinationItemKey: String
  )(implicit
    ctx: ExecutionContext): Future[CopyResult] = Future {

    val copyTransfer = transferManager.copy(
      sourceNamespace,
      sourceItemKey,
      destinationNamespace,
      destinationItemKey
    )

    copyTransfer.waitForCopyResult()
  }
}

object S3Copier {
  def apply()(implicit s3Client: AmazonS3) = {
    new S3Copier()
  }
}
