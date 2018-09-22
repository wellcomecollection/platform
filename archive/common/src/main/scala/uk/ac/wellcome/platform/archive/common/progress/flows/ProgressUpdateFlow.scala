package uk.ac.wellcome.platform.archive.common.progress.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

import scala.util.Try

object ProgressUpdateFlow {
  def apply(progressMonitor: ProgressMonitor)
  : Flow[ProgressUpdate,
    Either[FailedEvent[ProgressUpdate], ProgressUpdate],
    NotUsed] = {

    Flow[ProgressUpdate].map(progressUpdate => {
      Try(progressMonitor.update(progressUpdate)).toEither
        .left.map(e => FailedEvent(e, progressUpdate))
        .right.map(_ => progressUpdate)
    })

  }
}
