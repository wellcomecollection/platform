package uk.ac.wellcome.platform.idminter.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import io.circe.Json
import uk.ac.wellcome.messaging.message.{MessageStream, MessageWriter}
import uk.ac.wellcome.platform.idminter.steps.IdEmbedder

import scala.concurrent.{ExecutionContext, Future}

class IdMinterWorkerService @Inject()(
  idEmbedder: IdEmbedder,
  writer: MessageWriter[Json],
  messageStream: MessageStream[Json],
  system: ActorSystem
)(implicit ec: ExecutionContext) {

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(json: Json): Future[Unit] =
    for {
      identifiedJson <- idEmbedder.embedId(json)
      _ <- writer.write(
        message = identifiedJson,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()

  def stop(): Future[Terminated] =
    system.terminate()
}
