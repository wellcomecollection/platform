package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Json
import io.circe.parser._
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.messaging.sqs.{SQSMessage, SQSReader, SQSWorker}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.platform.idminter.steps.IdEmbedder
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class IdMinterWorkerService @Inject()(
  idEmbedder: IdEmbedder,
  writer: SNSWriter,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override lazy val poll = 100 milliseconds
  val snsSubject = "identified-item"

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      json <- Future.fromTry(parseMessageIntoJson(message))
      workWithCanonicalId <- idEmbedder.embedId(json)
      _ <- writer.writeMessage(
        message = workWithCanonicalId.toString(),
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()

  private def parseMessageIntoJson(message: SQSMessage): Try[Json] = {
    Try {
      parse(message.body) match {
        case Right(json) => json
        case Left(exception) =>
          error(s"error parsing message into json: $exception")
          throw exception
      }
    }
  }
}
