package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class IdMinterWorkerService @Inject()(
  identifiedWorkIndexer: IdentifiedWorkIndexer,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker {

  override val sqsReader: SQSReader = reader
  override val actorSystem: ActorSystem = system
  override val metricsSender: MetricsSender = metrics

  override def processMessage(message: SQSMessage): Future[Unit] =
    identifiedWorkIndexer.indexIdentifiedWork(message.body).map(_ => ())

}
