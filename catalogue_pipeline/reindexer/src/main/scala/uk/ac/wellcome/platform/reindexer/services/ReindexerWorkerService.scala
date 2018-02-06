package uk.ac.wellcome.platform.reindexer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.JsonUtil._

class ReindexerWorkerService @Inject()(
  targetService: ReindexTargetService,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] = Future {
    fromJson[ReindexJob](message.body) match {
      case Success(job) => targetService.runReindex(job = job).map(_ => ())
      case Failure(err) => throw GracefulFailureException(err)
    }
  }
}
