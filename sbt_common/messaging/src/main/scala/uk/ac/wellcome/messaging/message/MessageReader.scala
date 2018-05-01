package uk.ac.wellcome.messaging.message

import com.amazonaws.services.sqs
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.utils.JsonUtil._
import com.google.inject.Inject
import uk.ac.wellcome.s3.S3ObjectStore

import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class MessageReader[T] @Inject()(s3ObjectStore: S3ObjectStore[T]) {

  def read(message: sqs.model.Message)(
    implicit decoderN: Decoder[NotificationMessage],
    decoderT: Decoder[T]): Future[T] = {
    val deserialisedMessagePointerAttempt = for {
      notification <- fromJson[NotificationMessage](message.getBody)
      deserialisedMessagePointer <- fromJson[MessagePointer](
        notification.Message)
    } yield deserialisedMessagePointer

    for {
      messagePointer <- Future.fromTry[MessagePointer](
        deserialisedMessagePointerAttempt)
      deserialisedObject <- s3ObjectStore.get(messagePointer.src)
    } yield deserialisedObject
  }

}
