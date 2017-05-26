package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.twitter.inject.Logging
import uk.ac.wellcome.models.Reindexable
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService[T <: Reindexable[String]] @Inject()(
  reindexTrackerService: ReindexTrackerService,
  reindexTargetService: ReindexTargetService[T])
    extends Logging {

  def run: Future[PutItemResult] =
    for {
      indices <- reindexTrackerService.getIndicesForReindex
      attempt = indices.map(ReindexAttempt(_, Nil, 0))
      _ <- attempt.map(processReindexAttempt).get
      updates <- reindexTrackerService.updateReindex(attempt.get)
    } yield updates

  private def processReindexAttempt(
    reindexAttempt: ReindexAttempt): Future[ReindexAttempt] =
    reindexAttempt match {
      case ReindexAttempt(_, _, attempt) if attempt > 2 =>
        Future.failed(
          new RuntimeException(
            s"Giving up on $reindexAttempt, tried too many times.")) // Stop: give up!
      case ReindexAttempt(reindex, Nil, attempt) if attempt != 0 =>
        Future.successful(ReindexAttempt(reindex, Nil, attempt)) // Stop: done!
      case _ =>
        reindexTargetService
          .runReindex(reindexAttempt)
          .flatMap(processReindexAttempt) // Carry on.
    }
}
