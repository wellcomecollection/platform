package uk.ac.wellcome.messaging.message

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.messaging.GlobalExecutionContext.context
import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

case class MessageWriterConfig(
                                snsConfig: SNSConfig,
                                s3Config: S3Config
                              )

class MessageWriter[T, S <: MessageSender[T]] @Inject()(
  snsWriter: SNSWriter,
  messageSender: S
) extends Logging {

  def write(message: T, subject: String)(
    implicit encoder: Encoder[T]): Future[Unit] = {
    for {
      publishAttempt <- messageSender.send(message, subject)
      _ = info(publishAttempt)
    } yield ()

  }
}
