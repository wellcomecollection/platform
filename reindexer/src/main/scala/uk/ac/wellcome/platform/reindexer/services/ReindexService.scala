package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Reindexable
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService[T <: Reindexable[String]] @Inject()(
  reindexTrackerService: ReindexTrackerService,
  reindexTargetService: ReindexTargetService[T],
  metricsSender: MetricsSender)
    extends Logging {

  def run: Future[PutItemResult] = {
    info("ReindexService started.")

    metricsSender.timeAndCount("reindex-total-time", () => {
      val result = for {
        indices <- reindexTrackerService.getIndexForReindex
        attempt = indices.map(ReindexAttempt(_, Nil, 0))
        _ <- attempt.map(processReindexAttempt).getOrElse(Future.successful((): Unit))
        updates <- reindexTrackerService.updateReindex(attempt.get)
      } yield updates

      info("ReindexService finished.")

      result
    })
  }

  private def processReindexAttempt(
    reindexAttempt: ReindexAttempt): Future[ReindexAttempt] =
    reindexAttempt match {
      case ReindexAttempt(_, _, attempt) if attempt > 2 =>
        Future.failed(
          new RuntimeException(
            s"Giving up on $reindexAttempt, tried too many times.")) // Stop: give up!
      case ReindexAttempt(reindex, Nil, attempt) if attempt != 0 => {
        info(s"Finshed reindexing $reindex successfully.")
        Future.successful(ReindexAttempt(reindex, Nil, attempt)) // Stop: done!
      }
      case _ => {
        info(s"ReindexService continuing to process $reindexAttempt")

        reindexTargetService
          .runReindex(reindexAttempt)
          .flatMap(processReindexAttempt) // Carry on.
      }
    }
}
