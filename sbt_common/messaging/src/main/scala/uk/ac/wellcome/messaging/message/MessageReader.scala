package uk.ac.wellcome.messaging.message

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs
import com.amazonaws.services.sqs.AmazonSQS
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.utils.JsonUtil._
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.storage.s3.{KeyPrefixGenerator, S3Config, S3ObjectStore}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

case class MessageReaderConfig(sqsConfig: SQSConfig, s3Config: S3Config)

import scala.concurrent.Future

class MessageReader[T] @Inject()(
  messageReaderConfig: MessageReaderConfig,
  s3Client: AmazonS3,
  sqsClient: AmazonSQS,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) {
  val sqsReader = new SQSReader(sqsClient, messageReaderConfig.sqsConfig)

  val s3ObjectStore = new S3ObjectStore[T](
    s3Client = s3Client,
    s3Config = messageReaderConfig.s3Config,
    keyPrefixGenerator = keyPrefixGenerator
  )

  def readAndDelete(f: T => Future[Unit])(
    implicit decoderN: Decoder[NotificationMessage],
    decoderT: Decoder[T]
  ): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      for {
        t <- read(message)
        r <- f(t)
      } yield r
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
