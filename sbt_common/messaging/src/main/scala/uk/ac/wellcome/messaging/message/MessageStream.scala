package uk.ac.wellcome.messaging.message

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.s3.S3ObjectStore
import uk.ac.wellcome.utils.JsonUtil.{fromJson, _}

import scala.concurrent.Future

class MessageStream[T] @Inject()(actorSystem: ActorSystem,
                                 sqsClient: AmazonSQSAsync,
                                 s3Client: AmazonS3,
                                 messageReaderConfig: MessageReaderConfig,
                                 metricsSender: MetricsSender)
    extends SQSStream[T](
      actorSystem = actorSystem,
      sqsClient = sqsClient,
      sqsConfig = messageReaderConfig.sqsConfig,
      metricsSender = metricsSender
    ) {

  val s3ObjectStore = new S3ObjectStore[T](
    s3Client = s3Client,
    s3Config = messageReaderConfig.s3Config
  )

  override def read(message: sqs.model.Message)(implicit decoderT: Decoder[T]): Future[T] = {
    val deserialisedMessagePointerAttempt = for {
      notification <- fromJson[NotificationMessage](message.getBody)
      deserialisedMessagePointer <- fromJson[MessagePointer](
        notification.Message)
    } yield deserialisedMessagePointer

    for {
      messagePointer <- Future.fromTry[MessagePointer](
        deserialisedMessagePointerAttempt)
      deserialisedObject <- s3ObjectStore.get(messagePointer.src)
    } yield deserialisedObject
  }
}
