package uk.ac.wellcome.messaging.message

import java.net.URI

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.models.aws.S3Config

import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{blocking, Future}

class MessageWriter[T] @Inject()(
  private val sns: SNSWriter,
  private val s3Config: S3Config,
  private val s3: S3ObjectStore[T]
) extends Logging {

  def write(message: T, subject: String)(
    implicit encoder: Encoder[T]): Future[Unit] = {

    val bucket = s3Config.bucketName

    for {
      location <- s3.put(message)
      pointer <- Future.fromTry(toJson(MessagePointer(location)))
      publishAttempt <- sns.writeMessage(pointer, subject)
      _ = info(publishAttempt)
    } yield ()

  }
}
