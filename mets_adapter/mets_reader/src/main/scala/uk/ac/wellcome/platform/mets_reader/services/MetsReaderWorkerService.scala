package uk.ac.wellcome.platform.mets_reader.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import uk.ac.wellcome.messaging.sqs._

import scala.concurrent.Future

class MetsReaderWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[SQSMessage]
) extends Logging {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val decoderT: Decoder[SQSMessage] = deriveDecoder[SQSMessage]

  sqsStream.foreach(
    streamName = this.getClass.getSimpleName,
    process = processMessage
  )

  def processMessage(sqsMessage: SQSMessage): Future[Unit] =
    Future { println("I got a message!") }
}
