package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.util.{Failure, Success}

class IngestorWorkerService @Inject()(
  identifiedWorkIndexer: WorkIndexer,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    fromJson[Work](message.body) match {
      case Success(work) =>
        identifiedWorkIndexer.indexWork(work = work).map(_ => ())
      case Failure(err) => Future {
        throw GracefulFailureException(err)
      }
    }
}
