package uk.ac.wellcome.platform.archive.common

import java.io.File

import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.transfer.TransferManager
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{Future, Promise}

object TemporaryStore extends Logging {
  val tmpFilePrefix = "wellcome-tmp-"
  val tmpFileSuffix = ".tmp"


  implicit class TemporaryStoreOps(location: ObjectLocation) {
    private val transferSuccessfulEvents = Set(
      ProgressEventType.TRANSFER_COMPLETED_EVENT)
    private val transferFailedEvents = Set(
      ProgressEventType.TRANSFER_CANCELED_EVENT,
      ProgressEventType.TRANSFER_FAILED_EVENT)

    def downloadTempFile(implicit transferManager: TransferManager): Future[File] = {
      val tmpFile = File.createTempFile(
        tmpFilePrefix,
        tmpFileSuffix
      )
      val download =transferManager.download(location.namespace, location.key, tmpFile)

      val promise = Promise[File]()
      download.addProgressListener(new ProgressListener {
        override def progressChanged(progressEvent: ProgressEvent): Unit = if (transferSuccessfulEvents.contains(progressEvent.getEventType)) {
          promise trySuccess tmpFile
        } else if (transferFailedEvents.contains(progressEvent.getEventType)) {
          promise failure download.waitForException()
        }
      })
      promise.future

  }
  }
}
