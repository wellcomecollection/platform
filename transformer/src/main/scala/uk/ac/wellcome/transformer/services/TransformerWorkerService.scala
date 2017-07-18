package uk.ac.wellcome.transformer.services

import akka.actor.ActorSystem
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}

import scala.concurrent.Future

import uk.ac.wellcome.utils.GlobalExecutionContext.context

class TransformerWorkerService(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker {

  override val sqsReader: SQSReader = reader
  override val actorSystem: ActorSystem = system
  override val metricsSender: MetricsSender = metrics

  override def processMessage(message: SQSMessage): Future[Unit] = Future {}
}
