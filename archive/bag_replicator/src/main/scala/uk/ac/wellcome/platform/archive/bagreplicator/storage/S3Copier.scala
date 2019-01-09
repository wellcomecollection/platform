package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.model.CopyResult

import scala.concurrent.{Future, Promise}

class S3Copier(implicit s3Client: AmazonS3) {
  import com.amazonaws.services.s3.transfer.TransferManagerBuilder
  private val transferManager = TransferManagerBuilder.standard
    .withS3Client(s3Client)
    .build // Fixed thread pool
  private val transferSuccessfulEvents = Set(
    ProgressEventType.TRANSFER_COMPLETED_EVENT)
  private val transferFailedEvents = Set(
    ProgressEventType.TRANSFER_CANCELED_EVENT,
    ProgressEventType.TRANSFER_FAILED_EVENT)

  def copy(sourceNamespace: String,
           sourceItemKey: String,
           destinationNamespace: String,
           destinationItemKey: String): Future[CopyResult] = {
    val copyTransfer =
      transferManager.copy(
        sourceNamespace,
        sourceItemKey,
        destinationNamespace,
        destinationItemKey)

    val promisedCopy = Promise[CopyResult]()
    copyTransfer.addProgressListener(new ProgressListener {
      override def progressChanged(progressEvent: ProgressEvent): Unit = {
        if (transferSuccessfulEvents.contains(progressEvent.getEventType)) {
          promisedCopy trySuccess copyTransfer.waitForCopyResult()
        } else if (transferFailedEvents.contains(progressEvent.getEventType)) {
          promisedCopy failure copyTransfer.waitForException()
        }
      }
    })
    promisedCopy.future
  }
}
