package uk.ac.wellcome.message

import java.net.URI

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.models.aws.S3Config
import com.amazonaws.services.s3.AmazonS3
import java.util.UUID.randomUUID

import io.circe.Encoder
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.sns.SNSWriter

import scala.concurrent.{Future, blocking}

class MessageWriter[T] @Inject()(
  private val sns: SNSWriter,
  private val s3Config: S3Config,
  private val s3: S3ObjectStore[T]
) extends Logging {

  def write(message: T, subject: String)(implicit encoder: Encoder[T]): Future[Unit] = {

    val bucket= s3Config.bucketName

    for {
      key <- s3.put(message)
      pointer <- Future.fromTry(toJson(MessagePointer(s"s3://$bucket/$key")))
      publishResult <- sns.writeMessage(pointer, subject)
    } yield ()

  }
}
