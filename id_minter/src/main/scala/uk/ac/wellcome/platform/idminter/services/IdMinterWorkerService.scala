package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.steps.{SomethingSomethin, WorkExtractor}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class IdMinterWorkerService @Inject()(
                                       identificator: SomethingSomethin,
                                       writer: SNSWriter,
                                       reader: SQSReader,
                                       system: ActorSystem,
                                       metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  val snsSubject = "identified-item"

  private def toWorkJson(work: Work, canonicalId: String) = {
    JsonUtil.toJson(work.copy(canonicalId = Some(canonicalId))).get
  }

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      work <- WorkExtractor.toWork(message)
      canonicalId <- identificator.generateId(work)
      _ <- writer.writeMessage(toWorkJson(work, canonicalId), Some(snsSubject))
    } yield ()

}
