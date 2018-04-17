package uk.ac.wellcome.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.models.aws.S3Config
import com.amazonaws.services.s3.AmazonS3
import java.util.UUID.randomUUID

import scala.concurrent.{blocking, Future}
import uk.ac.wellcome.utils.JsonUtil._
import scala.util.{Failure, Success, Try}
import uk.ac.wellcome.sqs.MessagePointer

class SNSWriter @Inject()(
  client: AmazonSNS,
  config: SNSConfig,
  s3Client: AmazonS3,
  s3Config: S3Config
) extends Logging {

  private val arn = config.topicArn
  private val bucket = s3Config.bucketName

  def writeMessage(message: String, subject: String): Future[Unit] = {

    val contentId = randomUUID.toString
    // TODO make key prefix configurable
    val key = s"messages/$contentId"

    for {
      _ <- eventuallyStoreMessage(message, key)
      pointer <- toFuture(toJson(MessagePointer(s"s3://$bucket/$key")))
      publishResult <- eventuallyPublishMessagePointer(pointer, subject, key)
    } yield {
      info(s"Published message ${publishResult.getMessageId}")
      ()
    }

  }

  private def eventuallyStoreMessage(message: String, key: String) = Future {
    blocking {
      debug(s"storing message s3://$bucket/$key")
      s3Client.putObject(bucket, key, message)
    }
  }

  private def eventuallyPublishMessagePointer(message: String,
                                              subject: String,
                                              key: String) = Future {
    blocking {
      debug(s"publishing message to SNS topic $arn")
      val request = new PublishRequest(arn, message, subject)
      client.publish(request)
    }
  }

  private def toFuture[T](t: Try[T]): Future[T] = t match {
    case Success(value) => Future.successful(value)
    case Failure(ex) => Future.failed(ex)
  }

}
