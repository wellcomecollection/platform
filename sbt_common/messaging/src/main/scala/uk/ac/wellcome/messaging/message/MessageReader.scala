package uk.ac.wellcome.messaging.message

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.utils.JsonUtil._
import com.google.inject.Inject
import uk.ac.wellcome.storage.s3.S3ObjectReader
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class MessageReader[T] @Inject()(
  messageConfig: MessageConfig,
  s3Client: AmazonS3
) {

  val s3ObjectReader = new S3ObjectReader[T](
    s3Client = s3Client,
    s3Config = messageConfig.s3Config
  )

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
      deserialisedObject <- s3ObjectReader.get(messagePointer.src)
    } yield deserialisedObject
  }
}
