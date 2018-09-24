package uk.ac.wellcome.platform.archive.common.progress.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

object ProgressUpdateFlow {
  def apply(progressMonitor: ProgressMonitor)
    : Flow[ProgressUpdate,
           Either[FailedEvent[ProgressUpdate], Progress],
           NotUsed] = {

    Flow[ProgressUpdate].map(progressUpdate => {
      progressMonitor.update(progressUpdate)
    })

  }
}
