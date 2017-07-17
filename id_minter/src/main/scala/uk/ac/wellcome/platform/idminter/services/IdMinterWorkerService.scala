package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifiedWork, Work}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.steps.{
  IdentifierGenerator,
  WorkExtractor
}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class IdMinterWorkerService @Inject()(
  idGenerator: IdentifierGenerator,
  writer: SNSWriter,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker {

  override val sqsReader: SQSReader = reader
  override val actorSystem: ActorSystem = system
  override val metricsSender: MetricsSender = metrics

  val snsSubject = "identified-item"

  private def toIdentifiedWorkJson(work: Work, canonicalId: String) = {
    JsonUtil.toJson(IdentifiedWork(canonicalId, work)).get
  }

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      work <- WorkExtractor.toWork(message)
      canonicalId <- idGenerator.generateId(work)
      _ <- writer.writeMessage(toIdentifiedWorkJson(work, canonicalId),
                               Some(snsSubject))
    } yield ()

}
