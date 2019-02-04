package uk.ac.wellcome.platform.archive.archivist.models

import java.io.File

import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.transfer.TransferManager
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

object TemporaryStore extends Logging {
  val tmpFilePrefix = "wellcome-tmp-"
  val tmpFileSuffix = ".tmp"

  implicit class TemporaryStoreOps(location: ObjectLocation) {
    private val transferSuccessfulEvents = Set(
      ProgressEventType.TRANSFER_COMPLETED_EVENT)
    private val transferFailedEvents = Set(
      ProgressEventType.TRANSFER_CANCELED_EVENT,
      ProgressEventType.TRANSFER_FAILED_EVENT)

    def downloadTempFile(
      implicit transferManager: TransferManager): Future[File] = {
      val tmpFile = File.createTempFile(
        tmpFilePrefix,
        tmpFileSuffix
      )
      tmpFile.deleteOnExit()
      debug(s"Storing $location @ ${tmpFile.getAbsolutePath}")
      val triedDownload =
        Try(transferManager.download(location.namespace, location.key, tmpFile))
      val promise = Promise[File]()
      triedDownload match {
        case Success(download) =>
          download.addProgressListener(
            new ProgressListener {
              override def progressChanged(progressEvent: ProgressEvent): Unit =
                if (transferSuccessfulEvents.contains(
                      progressEvent.getEventType)) {
                  promise success tmpFile
                } else if (transferFailedEvents.contains(
                             progressEvent.getEventType)) {
                  promise failure download.waitForException()
                }
            })
        case Failure(exception) => promise failure exception
      }
      promise.future

    }
  }
}
