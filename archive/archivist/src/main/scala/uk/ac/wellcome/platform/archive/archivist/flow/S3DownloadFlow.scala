package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File

import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.transfer.{Download, TransferManager}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.errors.FileDownloadingError
import uk.ac.wellcome.platform.archive.common.models.{
  FileDownloadComplete,
  IngestBagRequest
}

import scala.util.{Failure, Try}

object S3DownloadFlow extends Logging {
  def apply(implicit transferManager: TransferManager) =
    new S3DownloadFlow
}

class S3DownloadFlow(implicit transferManager: TransferManager)
    extends GraphStage[
      FlowShape[IngestBagRequest,
                Either[FileDownloadingError, FileDownloadComplete]]]
    with Logging {

  private val transferSuccessfulEvents = Set(
    ProgressEventType.TRANSFER_COMPLETED_EVENT)
  private val transferFailedEvents = Set(
    ProgressEventType.TRANSFER_CANCELED_EVENT,
    ProgressEventType.TRANSFER_FAILED_EVENT)

  val in = Inlet[IngestBagRequest]("S3DownloadFlow.in")
  val out = Outlet[Either[FileDownloadingError, FileDownloadComplete]](
    "S3DownloadFlow.out")

  override val shape = FlowShape.of(in, out)

  case class DownloadToFile(request: IngestBagRequest,
                            download: Download,
                            file: File)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private val tmpFilePrefix = "wellcome-tmp-"
      private val tmpFileSuffix = ".tmp"
      private var maybeDownloadToFile: Option[DownloadToFile] = None

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          maybeDownloadToFile match {
            case Some(_) =>
              info("out: Received pull, ongoing download so doing nothing")
              ()
            case None    =>
              info("out: Received pull, no ongoing download so pulling from inlet")
              pull(in)
          }
        }
        override def onDownstreamFinish(): Unit = ()
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          maybeDownloadToFile match {
            case None =>
              info("in: Received push, no download in progress so starting download")
              val request = grab(in)
              startDownload(request)
            case Some(_) =>
              info("in: Received push, download in progress, so doing nothing")
              ()
          }

        }
      })

      private def startDownload(request: IngestBagRequest): Unit = {
        val sourceLocation = request.zippedBagLocation
        val triedDownloadToFile = for {
          tmpFile <- createTempFile()
          _ = info(s"Storing $sourceLocation @ ${tmpFile.getAbsolutePath}")
          download <- Try(
            transferManager
              .download(sourceLocation.namespace, sourceLocation.key, tmpFile))
        } yield DownloadToFile(request, download, tmpFile)

        triedDownloadToFile.recoverWith {
          case ex: Throwable =>
            handleFailure(ex)
            Failure(ex)
        }
        triedDownloadToFile.foreach { downloadToFile =>
          maybeDownloadToFile = Some(downloadToFile)
          downloadToFile.download.addProgressListener(
            createProgressListener(downloadToFile))
        }
      }

      private def createProgressListener(downloadToFile: DownloadToFile) =
        new ProgressListener {
          override def progressChanged(progressEvent: ProgressEvent): Unit =
            if (transferSuccessfulEvents.contains(progressEvent.getEventType)) {
              push(
                out,
                Right(
                  FileDownloadComplete(
                    downloadToFile.file,
                    downloadToFile.request)))
              completeStage()
            } else if (transferFailedEvents.contains(
                         progressEvent.getEventType)) {
              handleFailure(downloadToFile.download.waitForException())
            }
        }
      private def createTempFile() = Try {
        val tmpFile = File.createTempFile(
          tmpFilePrefix,
          tmpFileSuffix
        )

        tmpFile.deleteOnExit()
        tmpFile
      }

      private def handleFailure(ex: Throwable): Unit = {
        error("There was a failure downloading from s3!", ex)
        maybeDownloadToFile match {
          case Some(DownloadToFile(request, download, tmpFile)) =>
            Try {
              download.abort()
              tmpFile.delete()
            }
            push(out, Left(FileDownloadingError(request, ex)))
          case None =>
        }
        val supervisionStrategy = inheritedAttributes.get[SupervisionStrategy](
          SupervisionStrategy(_ => Supervision.Stop))
        supervisionStrategy.decider(ex) match {
          case Supervision.Stop    => failStage(ex)
          case Supervision.Resume  => completeStage()
          case Supervision.Restart => completeStage()
        }
      }

    }
}
