package uk.ac.wellcome.platform.archive.registrar.flows

import akka.stream._
import akka.stream.stage.{InHandler, OutHandler, _}
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ArchiveProgressMonitor
import uk.ac.wellcome.platform.archive.registrar.models.RegisterRequestContext

case class RecordArchiveProgressEventFlow[T](eventDescription: String, status: Option[ArchiveProgress.Status]=None)(implicit archiveProgressMonitor: ArchiveProgressMonitor)
  extends GraphStage[FlowShape[(T, RegisterRequestContext), (T, RegisterRequestContext)]] {

  val in: Inlet[(T, RegisterRequestContext)] = Inlet[(T, RegisterRequestContext)]("InitialiseArchiveProgress.in")
  val out: Outlet[(T, RegisterRequestContext)] = Outlet[(T, RegisterRequestContext)]("InitialiseArchiveProgress.out")
  override val shape: FlowShape[(T, RegisterRequestContext), (T, RegisterRequestContext)] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler with StageLogging {

      override def onPush(): Unit = {
                    val inflow = grab(in)
                    val context = inflow._2

                    archiveProgressMonitor.addEvent(context.requestId.toString, eventDescription, status)

                    push(out, inflow)
                  }

      override def onPull(): Unit = pull(in)

      setHandlers(in, out, this)
    }
}



