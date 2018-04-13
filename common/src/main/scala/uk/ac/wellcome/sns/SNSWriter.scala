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

case class PublishAttempt(id: String, key: String)

class SNSWriter @Inject()(
  client: AmazonSNS,
  config: SNSConfig,
  s3Client: AmazonS3,
  s3Config: S3Config
) extends Logging {

  def writeMessage(
    message: String,
    subject: String): Future[Either[Throwable, PublishAttempt]] = {

    val contentId = randomUUID.toString
    // TODO make key prefix configurable
    val key = s"messages/$contentId"

    (for {
      _ <- eventuallyStoreMessage(message, key)
      publishResult <- eventuallyPublishMessagePointer(subject)
    } yield {
      info(s"Published message ${publishResult.getMessageId}")
      Right(PublishAttempt(publishResult.getMessageId, key))
    }) recover {
      case e: Throwable => Left(e)
    }

  }

  private def eventuallyStoreMessage(message: String, key: String) = Future {
    blocking {
      debug(s"storing message s3://${s3Config.bucketName}/$key")
      s3Client.putObject(s3Config.bucketName, key, message)
    }
  }

  private def eventuallyPublishMessagePointer(subject: String) = Future {
    blocking {
      debug(s"publishing message to SNS topic ${config.topicArn}")
      val request =
        new PublishRequest(config.topicArn, s3Config.bucketName, subject)
      client.publish(request)
    }
  }

}
