package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.parser._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.steps.IdEmbedder
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.util.Try

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
      json <- Future.fromTry(Try(parse(message.body).right.get))
      workWithCanonicalId <- idEmbedder.embedId(json)
      _ <- writer.writeMessage(workWithCanonicalId.toString(),
                               Some(snsSubject))
    } yield ()

}
