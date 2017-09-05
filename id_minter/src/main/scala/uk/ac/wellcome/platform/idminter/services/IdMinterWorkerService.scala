package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.steps.{IdEmbedder, WorkExtractor}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class IdMinterWorkerService @Inject()(
  idEmbedder: IdEmbedder,
  writer: SNSWriter,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  val snsSubject = "identified-item"

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      work <- WorkExtractor.toWork(message)
      workWithCanonicalId <- idEmbedder.embedId(work)
      _ <- writer.writeMessage(JsonUtil.toJson(workWithCanonicalId).get,
                               Some(snsSubject))
    } yield ()

}
