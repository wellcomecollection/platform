package uk.ac.wellcome.messaging.message

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.utils.JsonUtil.{fromJson, _}

import scala.concurrent.{ExecutionContext, Future}

class MessageStream[T] @Inject()(actorSystem: ActorSystem,
                                 sqsClient: AmazonSQSAsync,
                                 s3Client: AmazonS3,
                                 messageReaderConfig: MessageReaderConfig,
                                 metricsSender: MetricsSender)(
  implicit objectStore: ObjectStore[T],
  ec: ExecutionContext) {

  private val sqsStream = new SQSStream[NotificationMessage](
    actorSystem = actorSystem,
    sqsClient = sqsClient,
    sqsConfig = messageReaderConfig.sqsConfig,
    metricsSender = metricsSender
  )

  def runStream(
    streamName: String,
    modifySource: Source[(Message, T), NotUsed] => Source[Message, NotUsed])
    : Future[Done] =
    sqsStream.runStream(
      streamName,
      source => modifySource(messageFromS3Source(source)))

  def foreach(streamName: String, process: T => Future[Unit]): Future[Done] =
    sqsStream.foreach(
      streamName = streamName,
      process = (notification: NotificationMessage) =>
        for {
          obj <- readFromS3(notification.Message)
          result <- process(obj)
        } yield result
    )

  private def messageFromS3Source(
    source: Source[(Message, NotificationMessage), NotUsed]) = {
    source.mapAsyncUnordered(messageReaderConfig.sqsConfig.parallelism) {
      case (message, notification) =>
        for {
          deserialisedObject <- readFromS3(notification.Message)
        } yield (message, deserialisedObject)
    }
  }

  private def readFromS3(messageString: String): Future[T] =
    for {
      messagePointer <- Future.fromTry(fromJson[MessagePointer](messageString))
      deserialisedObject <- objectStore.get(messagePointer.src)
    } yield deserialisedObject
}
