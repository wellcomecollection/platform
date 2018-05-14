package uk.ac.wellcome.messaging.message

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.storage.s3.{KeyPrefixGenerator, S3Config, S3TypedObjectStore}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

case class MessageWriterConfig(
  snsConfig: SNSConfig,
  s3Config: S3Config
)

class MessageWriter[T] @Inject()(
  messageConfig: MessageWriterConfig,
  snsClient: AmazonSNS,
  s3Client: AmazonS3,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) extends Logging {

  private val sns = new SNSWriter(
    snsClient = snsClient,
    snsConfig = messageConfig.snsConfig
  )

  private val s3 = new S3TypedObjectStore[T](
    s3Client = s3Client,
    s3Config = messageConfig.s3Config
  )

  def write(message: T, subject: String)(
    implicit encoder: Encoder[T]): Future[Unit] = {
    for {
      location <- s3.put(message, keyPrefixGenerator.generate(message))
      pointer <- Future.fromTry(toJson(MessagePointer(location)))
      publishAttempt <- sns.writeMessage(pointer, subject)
      _ = info(publishAttempt)
    } yield ()

  }
}
