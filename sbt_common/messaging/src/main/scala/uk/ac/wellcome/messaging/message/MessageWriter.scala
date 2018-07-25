package uk.ac.wellcome.messaging.message

import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSConfig, SNSWriter}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.{KeyPrefix, ObjectStore}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

case class MessageWriterConfig(
  snsConfig: SNSConfig,
  s3Config: S3Config
)

class MessageWriter[T] @Inject()(
  messageConfig: MessageWriterConfig,
  snsClient: AmazonSNS,
  s3Client: AmazonS3
)(implicit objectStore: ObjectStore[T], ec: ExecutionContext)
    extends Logging {

  private val sns = new SNSWriter(
    snsClient = snsClient,
    snsConfig = messageConfig.snsConfig
  )

  private val dateFormat = new SimpleDateFormat("YYYY/MM/dd")

  private def getKeyPrefix(): String = {
    val topicName = messageConfig.snsConfig.topicArn.split(":").last
    val currentTime = new Date()
    s"$topicName/${dateFormat.format(currentTime)}/${currentTime.getTime.toString}"
  }

  def write(message: T, subject: String): Future[PublishAttempt] = {
    for {
      location <- objectStore.put(messageConfig.s3Config.bucketName)(
        message,
        keyPrefix = KeyPrefix(getKeyPrefix())
      )
      _ = debug(s"Successfully stored message $message in location: $location")
      pointer <- Future.fromTry(toJson(MessagePointer(location)))
      publishAttempt <- sns.writeMessage(pointer, subject)
      _ = debug(publishAttempt)
    } yield publishAttempt

  }
}
