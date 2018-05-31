package uk.ac.wellcome.messaging.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SQSToDynamoStream[T] @Inject()(actorSystem: ActorSystem,
                                     sqsStream: SQSStream[NotificationMessage])
    extends Logging {
  implicit val executor = actorSystem.dispatcher

  def foreach(name: String, f: T => Future[Unit])(
    implicit decoderT: Decoder[T]) = {
    sqsStream.foreach(
      name,
      obj => processMessage(f, obj)
    )
  }

  private def processMessage(store: T => Future[Unit],
                             message: NotificationMessage)(
    implicit decoderT: Decoder[T]): Future[Unit] =
    for {
      t <- Future.fromTry(fromJson[T](message.Message))
      _ <- store(t).recover {
        case e: ConditionalCheckFailedException =>
          logger.warn(s"Processing $message failed a Conditional Update", e)
          throw GracefulFailureException(e)
      }
    } yield ()
}
