package uk.ac.wellcome.platform.idminter.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import io.circe.parser._
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.messaging.sqs.{SQSMessage, SQSReader, SQSWorker}
import uk.ac.wellcome.message.{MessageReader, MessageWorker}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.License.createLicense
import uk.ac.wellcome.models.{License, UnidentifiedWork}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.idminter.steps.IdEmbedder
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Try}

class IdMinterWorkerService @Inject()(
                                       idEmbedder: IdEmbedder,
                                       writer: SNSWriter,
                                       sqsReader: SQSReader,
                                       messageReader: MessageReader[Json],
                                       system: ActorSystem,
                                       metrics: MetricsSender
) extends MessageWorker[Json](sqsReader, messageReader, system, metrics) {

  override lazy val poll = 100 milliseconds
  val snsSubject = "identified-item"

  override implicit val decoder: Decoder[Json] = Decoder.instanceTry[Json] { hCursor =>
    Try(hCursor.value)
  }

  override def processMessage(json: Json): Future[Unit] =
    for {
      identifiedJson <- idEmbedder.embedId(json)
      _ <- writer.writeMessage(
        message = identifiedJson.toString(),
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()
}
