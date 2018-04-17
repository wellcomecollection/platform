package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import com.amazonaws.services.s3.AmazonS3

class IngestorWorkerService @Inject()(
  identifiedWorkIndexer: WorkIndexer,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  s3: AmazonS3
) extends SQSWorker(reader, system, metrics, s3) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      work <- Future.fromTry(fromJson[IdentifiedWork](message.body))
      _ <- identifiedWorkIndexer.indexWork(work = work)
    } yield ()
}
