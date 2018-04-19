package uk.ac.wellcome.message

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.models.aws.S3Config
import com.amazonaws.services.s3.AmazonS3
import java.util.UUID.randomUUID
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.sns.SNSWriter

import scala.concurrent.{blocking, Future}

class MessageWriter @Inject()(
  private val sns: SNSWriter,
  private val s3: AmazonS3,
  private val s3Config: S3Config
) extends Logging {

  private val bucket = s3Config.bucketName

  def write(message: String, subject: String): Future[Unit] = {

    val contentId = randomUUID.toString
    // TODO make key prefix configurable
    val key = s"messages/$contentId"

    for {
      _ <- eventuallyStoreMessage(message, key)
      pointer <- Future.fromTry(toJson(MessagePointer(s"s3://$bucket/$key")))
      publishResult <- sns.writeMessage(pointer, subject)
    } yield ()

  }

  private def eventuallyStoreMessage(message: String, key: String) = Future {
    blocking {
      debug(s"storing message s3://$bucket/$key")
      s3.putObject(bucket, key, message)
    }
  }

}
