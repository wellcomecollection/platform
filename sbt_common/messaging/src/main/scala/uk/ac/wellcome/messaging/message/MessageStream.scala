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
import uk.ac.wellcome.storage.s3.{S3StringStore, S3TypedObjectStore}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil.{fromJson, _}

import scala.concurrent.Future

class MessageStream[T] @Inject()(actorSystem: ActorSystem,
                                 sqsClient: AmazonSQSAsync,
                                 s3Client: AmazonS3,
                                 messageReaderConfig: MessageReaderConfig,
                                 metricsSender: MetricsSender) {

  private val s3StringStore = new S3StringStore(
    s3Client = s3Client,
    s3Config = messageReaderConfig.s3Config
  )

  private val s3TypedObjectStore = new S3TypedObjectStore[T](
    stringStore = s3StringStore
  )

  private val sqsStream = new SQSStream[NotificationMessage](
    actorSystem = actorSystem,
    sqsClient = sqsClient,
    sqsConfig = messageReaderConfig.sqsConfig,
    metricsSender = metricsSender
  )

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderT: Decoder[T],
    decoderN: Decoder[NotificationMessage]): Future[Done] = {
    sqsStream.foreach(
      streamName = streamName,
      process = (notification: NotificationMessage) =>
        processMessagePointer(notification, process)
    )
  }

  private def processMessagePointer(
    notification: NotificationMessage,
    process: T => Future[Unit])(implicit decoderT: Decoder[T]): Future[Unit] =
    for {
      messagePointer <- Future.fromTry(
        fromJson[MessagePointer](notification.Message))
      deserialisedObject <- s3TypedObjectStore.get(messagePointer.src)
      _ <- process(deserialisedObject)
    } yield ()
}
