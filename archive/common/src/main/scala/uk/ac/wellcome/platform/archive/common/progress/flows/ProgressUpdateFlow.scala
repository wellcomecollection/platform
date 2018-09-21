package uk.ac.wellcome.platform.archive.common.progress.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.progress.models.{
  FailedProgressUpdate,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

import scala.util.Try

object ProgressUpdateFlow {
  def apply(progressMonitor: ProgressMonitor)
    : Flow[ProgressUpdate,
           Either[FailedProgressUpdate, ProgressUpdate],
           NotUsed] = {

    Flow[ProgressUpdate].map(progressUpdate => {
      Try(progressMonitor.update(progressUpdate)).toEither.left
        .map(e => FailedProgressUpdate(e, progressUpdate))
        .right
        .map(_ => progressUpdate)
    })

  }
}
