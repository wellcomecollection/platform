package uk.ac.wellcome.platform.archive.progress_async.flows
import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.flows.ProcessLogDiscardFlow
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker

object ProgressUpdateFlow {
  def apply(progressTracker: ProgressTracker)
    : Flow[ProgressUpdate, Progress, NotUsed] = {
    ProcessLogDiscardFlow[ProgressUpdate, Progress]("sns_publish")(
      progressTracker.update)
  }
}
