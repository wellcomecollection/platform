package uk.ac.wellcome.messaging.message

import java.net.URI

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.models.aws.S3Config

import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{blocking, Future}

class MessageWriter[T] @Inject()(
  messageConfig: MessageConfig,
  snsClient: AmazonSNS,
  s3Client: AmazonS3,
  keyPrefixGenerator: KeyPrefixGenerator[T]
) extends Logging {

  private val sns = new SNSWriter(
    snsClient = snsClient,
    snsConfig = messageConfig.snsConfig
  )

  private val s3 = new S3ObjectStore[T](
    s3Client = s3Client,
    s3Config = messageConfig.s3Config,
    keyPrefixGenerator = keyPrefixGenerator
  )

  def write(message: T, subject: String)(
    implicit encoder: Encoder[T]): Future[Unit] = {

    val bucket = messageConfig.s3Config.bucketName

    for {
      location <- s3.put(message)
      pointer <- Future.fromTry(toJson(MessagePointer(location)))
      publishAttempt <- sns.writeMessage(pointer, subject)
      _ = info(publishAttempt)
    } yield ()

  }
}
