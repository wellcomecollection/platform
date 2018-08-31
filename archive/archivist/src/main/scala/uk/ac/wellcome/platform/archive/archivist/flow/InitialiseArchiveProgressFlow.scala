package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream._
import akka.stream.stage.{InHandler, OutHandler, _}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContext
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ArchiveProgress,
  ProgressEvent
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ArchiveProgressMonitor

case class InitialiseArchiveProgressFlow[T](eventDescription: String)(
  implicit archiveProgressMonitor: ArchiveProgressMonitor)
    extends GraphStage[
      FlowShape[(T, IngestRequestContext), (T, IngestRequestContext)]] {

  val in: Inlet[(T, IngestRequestContext)] =
    Inlet[(T, IngestRequestContext)]("InitialiseArchiveProgress.in")
  val out: Outlet[(T, IngestRequestContext)] =
    Outlet[(T, IngestRequestContext)]("InitialiseArchiveProgress.out")
  override val shape
    : FlowShape[(T, IngestRequestContext), (T, IngestRequestContext)] =
    FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler
    with StageLogging {

      override def onPush(): Unit = {
        val inflow = grab(in)
        val context = inflow._2

        val progress = ArchiveProgress(
          id = context.id.toString,
          uploadUrl = context.bagLocation.toString,
          callbackUrl = context.callbackUrl.map(_.toString),
          result = ArchiveProgress.Processing,
          events = Seq(ProgressEvent(eventDescription))
        )

        archiveProgressMonitor.initialize(progress)

        push(out, inflow)
      }

      override def onPull(): Unit = pull(in)

      setHandlers(in, out, this)
    }
}

object InitialiseArchiveProgressFlow {
  def apply[T](eventDescription: String)(
    implicit archiveProgressMonitor: ArchiveProgressMonitor) =
    new InitialiseArchiveProgressFlow[T](eventDescription: String)
}
