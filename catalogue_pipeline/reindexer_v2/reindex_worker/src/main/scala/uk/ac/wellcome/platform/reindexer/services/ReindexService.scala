package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.gu.scanamo.DynamoFormat
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.Reindexable
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.TryBackoff

import scala.concurrent.Future

class ReindexService[+T <: Reindexable[String]] @Inject()(
  reindexTrackerService: ReindexTrackerService,
  reindexTargetService: ReindexTargetService[T],
  metricsSender: MetricsSender)(implicit dynamoFormat: DynamoFormat[T])
    extends Logging
    with TryBackoff {

  def run: Future[Unit] = {
    info("ReindexService started.")

    metricsSender.timeAndCount(
      "reindex-total-time",
      () => {
        val attempt: Future[Option[ReindexAttempt]] = for {
          indices <- reindexTrackerService.getIndexForReindex
          attempt = indices.map(ReindexAttempt(_, false, 0))
          _ <- attempt
            .map(processReindexAttempt)
            .getOrElse(Future.successful((): Unit))
        } yield attempt

        attempt
          .map {
            case Some(reindexAttempt) =>
              reindexTrackerService.updateReindex(reindexAttempt)
            case None => ()
          }
          .map(_ => info("ReindexService finished."))
      }
    )
  }

  private def processReindexAttempt(
    reindexAttempt: ReindexAttempt): Future[ReindexAttempt] =
    reindexAttempt match {
      case ReindexAttempt(reindex, true, attempt) => {
        info(s"Finshed reindexing $reindex successfully.")
        Future.successful(ReindexAttempt(reindex, true, attempt)) // Stop: done!
      }
      case _ => {
        info(
          s"ReindexService continuing at attempt ${reindexAttempt.attempt}, ")

        reindexTargetService
          .runReindex(reindexAttempt)
          .flatMap(processReindexAttempt) // Carry on.
      }
    }
}
