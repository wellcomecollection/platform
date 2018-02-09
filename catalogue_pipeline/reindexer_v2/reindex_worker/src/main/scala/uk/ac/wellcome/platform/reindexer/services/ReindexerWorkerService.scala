package uk.ac.wellcome.platform.reindexer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindexer.models.ReindexJob
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ReindexerWorkerService @Inject()(
  targetService: ReindexTargetService,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] = Future {
    fromJson[ReindexJob](message.body) match {
      case Success(reindexJob) =>
        targetService.runReindex(reindexJob = reindexJob).map(_ => ())
      case Failure(err) => throw GracefulFailureException(err)
    }
  }
}
