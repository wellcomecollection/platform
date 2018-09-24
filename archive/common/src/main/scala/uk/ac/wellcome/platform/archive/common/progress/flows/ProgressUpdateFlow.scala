package uk.ac.wellcome.platform.archive.common.progress.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.flows.ProcessLogDiscardFlow
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

object ProgressUpdateFlow {
  def apply(progressMonitor: ProgressMonitor)
    : Flow[ProgressUpdate, Progress, NotUsed] = {
    ProcessLogDiscardFlow[ProgressUpdate, Progress]("sns_publish")(
      progressMonitor.update)
  }
}
