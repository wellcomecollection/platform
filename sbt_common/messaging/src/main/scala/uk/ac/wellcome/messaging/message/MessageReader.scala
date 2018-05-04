package uk.ac.wellcome.messaging.message


import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs
import uk.ac.wellcome.utils.JsonUtil._
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.storage.s3.{S3Config, S3ObjectStore}

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

case class MessageReaderConfig(sqsConfig: SQSConfig, s3Config: S3Config)


class MessageReader[T] @Inject()(
  messageReaderConfig: MessageReaderConfig,
  s3Client: AmazonS3,
  sqsClient: AmazonSQS
) {
  val sqsReader = new SQSReader(sqsClient, messageReaderConfig.sqsConfig)

  val s3ObjectStore = new S3ObjectStore[T](
    s3Client = s3Client,
    s3Config = messageReaderConfig.s3Config
  )

  def readAndDelete(process: T => Future[Unit])(
    implicit decoderN: Decoder[NotificationMessage],
    decoderT: Decoder[T]
  ): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      for {
        t <- read(message)
        result <- process(t)
      } yield result
    }
  }

  private def read(message: sqs.model.Message)(
    implicit decoderN: Decoder[NotificationMessage],
    decoderT: Decoder[T]
  ): Future[T] = {
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
