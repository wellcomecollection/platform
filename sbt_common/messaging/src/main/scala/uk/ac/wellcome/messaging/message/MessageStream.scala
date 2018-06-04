package uk.ac.wellcome.messaging.message

import akka.Done
import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.messaging.GlobalExecutionContext.context
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.utils.JsonUtil.{fromJson, _}

import scala.concurrent.Future

class MessageStream[T] @Inject()(
  actorSystem: ActorSystem,
  sqsClient: AmazonSQSAsync,
  s3Client: AmazonS3,
  messageReaderConfig: MessageReaderConfig,
  metricsSender: MetricsSender)(implicit objectStore: ObjectStore[T]) {

  private val sqsStream = new SQSStream[NotificationMessage](
    actorSystem = actorSystem,
    sqsClient = sqsClient,
    sqsConfig = messageReaderConfig.sqsConfig,
    metricsSender = metricsSender
  )

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderN: Decoder[NotificationMessage]): Future[Done] = {
    sqsStream.foreach(
      streamName = streamName,
      process = (notification: NotificationMessage) =>
        processMessagePointer(notification, process)
    )
  }

  private def processMessagePointer(notification: NotificationMessage,
                                    process: T => Future[Unit]): Future[Unit] =
    for {
      messagePointer <- Future.fromTry(
        fromJson[MessagePointer](notification.Message))
      deserialisedObject <- objectStore.get(messagePointer.src)
      _ <- process(deserialisedObject)
    } yield ()
}
