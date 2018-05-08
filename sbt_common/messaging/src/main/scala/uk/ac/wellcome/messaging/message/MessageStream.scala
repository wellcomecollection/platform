package uk.ac.wellcome.messaging.message

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.s3.S3ObjectStore
import uk.ac.wellcome.utils.JsonUtil.fromJson
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class MessageStream[T](actorSystem: ActorSystem,
                       sqsClient: AmazonSQSAsync,
                       s3Client: AmazonS3,
                       messageReaderConfig: MessageReaderConfig,
                       metricsSender: MetricsSender)
    extends Logging {
  val decider: Supervision.Decider = {
    case _: Exception => Supervision.Resume
    case _ => Supervision.Stop
  }
  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider))
  implicit val dispatcher = system.dispatcher

  val s3ObjectStore = new S3ObjectStore[T](
    s3Client = s3Client,
    s3Config = messageReaderConfig.s3Config
  )

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderT: Decoder[T]): Future[Done] =
    SqsSource(messageReaderConfig.sqsConfig.queueUrl)(sqsClient)
      .mapAsyncUnordered(10) { message =>
        val metricName = s"${streamName}_ProcessMessage"
        metricsSender.timeAndCount(
          metricName,
          () => readAndProcess(streamName, message, process))
      }
      .map { m =>
        (m, MessageAction.Delete)
      }
      .runWith(SqsAckSink(messageReaderConfig.sqsConfig.queueUrl)(sqsClient))

  private def readAndProcess(
    streamName: String,
    message: Message,
    process: T => Future[Unit])(implicit decoderT: Decoder[T]) = {
    val processMessageFuture = for {
      t <- read(message)
      _ <- process(t)
    } yield message

    processMessageFuture.onFailure {
      case exception: GracefulFailureException =>
        logger.warn(s"Failure processing message", exception)
      case exception: Exception =>
        logger.error(s"Failure while processing message.", exception)
        metricsSender.incrementCount(
          s"${streamName}_MessageProcessingFailure",
          1.0)
    }
    processMessageFuture
  }

  private def read(message: sqs.model.Message)(
    implicit decoderN: Decoder[NotificationMessage],
    decoderT: Decoder[T]
  ): Future[T] = {
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
