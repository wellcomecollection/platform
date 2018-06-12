package uk.ac.wellcome.messaging.message

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
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

  def runStream[M](streamName: String, f: Source[(Message,T),NotUsed] => Source[Message,M]) = sqsStream.runStream(streamName, source =>
    f(source.mapAsyncUnordered(10){case (message, notification) =>
      for {
        deserialisedObject <- deserialiseObject(notification.Message)
      } yield (message,deserialisedObject)
    })
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
      deserialisedObject <- deserialiseObject(notification.Message)
      _ <- process(deserialisedObject)
    } yield ()

  private def deserialiseObject(messageString: String) = for {
    messagePointer <- Future.fromTry(
      fromJson[MessagePointer](messageString))
    deserialisedObject <- objectStore.get(messagePointer.src)
  } yield deserialisedObject
}
