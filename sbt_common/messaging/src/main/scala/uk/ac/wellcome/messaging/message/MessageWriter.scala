package uk.ac.wellcome.messaging.message

import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.storage.s3.{S3Config, S3StorageBackend, S3TypeStore}
import uk.ac.wellcome.messaging.GlobalExecutionContext.context
import uk.ac.wellcome.storage.KeyPrefix
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

case class MessageWriterConfig(
  snsConfig: SNSConfig,
  s3Config: S3Config
)

class MessageWriter[T] @Inject()(
  messageConfig: MessageWriterConfig,
  snsClient: AmazonSNS,
  s3Client: AmazonS3
)(implicit encoder: Encoder[T], decoder: Decoder[T])
    extends Logging {

  private val sns = new SNSWriter(
    snsClient = snsClient,
    snsConfig = messageConfig.snsConfig
  )

  private val s3 = new S3TypeStore[T](
    new S3StorageBackend[Json](s3Client)
  )

  private val dateFormat = new SimpleDateFormat("YYYY/MM/dd")

  private def getKeyPrefix(): String = {
    val topicName = messageConfig.snsConfig.topicArn.split(":").last
    s"$topicName/${dateFormat.format(new Date())}"
  }

  def write(message: T, subject: String): Future[Unit] = {
    for {
      location <- s3.put(messageConfig.s3Config.bucketName)(
        message,
        keyPrefix = KeyPrefix(getKeyPrefix())
      )
      pointer <- Future.fromTry(toJson(MessagePointer(location)))
      publishAttempt <- sns.writeMessage(pointer, subject)
      _ = info(publishAttempt)
    } yield ()

  }
}
