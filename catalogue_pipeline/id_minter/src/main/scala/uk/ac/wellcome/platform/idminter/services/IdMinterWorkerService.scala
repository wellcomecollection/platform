package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.{Decoder, Json}
import uk.ac.wellcome.messaging.message.{
  MessageReader,
  MessageWorker,
  MessageWriter
}
import uk.ac.wellcome.messaging.sqs.SQSReader
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.idminter.steps.IdEmbedder
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class IdMinterWorkerService @Inject()(
                                       idEmbedder: IdEmbedder,
                                       writer: MessageWriter[Json],
                                       messageReader: MessageReader[Json],
                                       system: ActorSystem,
                                       metrics: MetricsSender
) extends MessageWorker[Json](messageReader, system, metrics) {

  override lazy val poll = 100 milliseconds
  val snsSubject = "identified-item"

  override implicit val decoder: Decoder[Json] = Decoder.decodeJson

  override def processMessage(json: Json): Future[Unit] =
    for {
      identifiedJson <- idEmbedder.embedId(json)
      _ <- writer.write(
        message = identifiedJson,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()
}
