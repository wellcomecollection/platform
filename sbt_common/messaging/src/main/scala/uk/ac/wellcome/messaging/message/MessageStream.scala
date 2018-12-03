package uk.ac.wellcome.messaging.message

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class MessageStream[T](sqsClient: AmazonSQSAsync,
                       sqsConfig: SQSConfig,
                       metricsSender: MetricsSender)(
  implicit actorSystem: ActorSystem,
  objectStore: ObjectStore[T],
  ec: ExecutionContext) {

  private val sqsStream = new SQSStream[NotificationMessage](
    sqsClient = sqsClient,
    sqsConfig = sqsConfig,
    metricsSender = metricsSender
  )

  def runStream(
    streamName: String,
    modifySource: Source[(Message, T), NotUsed] => Source[Message, NotUsed])(
    implicit decoder: Decoder[T]): Future[Done] =
    sqsStream.runStream(
      streamName,
      source => modifySource(messageFromS3Source(source)))

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoder: Decoder[T]): Future[Done] =
    sqsStream.foreach(
      streamName = streamName,
      process = (notification: NotificationMessage) =>
        for {
          body <- getBody(notification.body)
          result <- process(body)
        } yield result
    )

  private def messageFromS3Source(
    source: Source[(Message, NotificationMessage), NotUsed])(
    implicit decoder: Decoder[T]) = {
    source.mapAsyncUnordered(sqsConfig.parallelism) {
      case (message, notification) =>
        for {
          deserialisedObject <- getBody(notification.body)
        } yield (message, deserialisedObject)
    }
  }

  private def getBody(messageString: String)(
    implicit decoder: Decoder[T]): Future[T] =
    for {
      notification <- Future.fromTry(
        fromJson[MessageNotification](messageString))
      body <- notification match {
        case inlineNotification: InlineNotification =>
          Future.fromTry(fromJson[T](inlineNotification.jsonString))
        case remoteNotification: RemoteNotification =>
          objectStore.get(remoteNotification.location)
      }
    } yield body
}
