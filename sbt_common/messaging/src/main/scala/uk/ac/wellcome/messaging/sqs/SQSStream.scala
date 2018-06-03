package uk.ac.wellcome.messaging.sqs

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.amazonaws.services.sqs
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.utils.JsonUtil.fromJson

import scala.concurrent.Future
import scala.util.Try

class SQSStream[T] @Inject()(actorSystem: ActorSystem,
                             sqsClient: AmazonSQSAsync,
                             sqsConfig: SQSConfig,
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

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderT: Decoder[T]): Future[Done] =
    SqsSource(sqsConfig.queueUrl)(sqsClient)
      .mapAsyncUnordered(parallelism = sqsConfig.parallelism) { message =>
        val metricName = s"${streamName}_ProcessMessage"
        metricsSender.timeAndCount(
          metricName,
          () => readAndProcess(streamName, message, process))
      }
      .map { m =>
        info(s"Deleting message ${m.getMessageId}")
        (m, MessageAction.Delete)
      }
      .runWith(SqsAckSink(sqsConfig.queueUrl)(sqsClient))

  private def readAndProcess(
    streamName: String,
    message: Message,
    process: T => Future[Unit])(implicit decoderT: Decoder[T]) = {
    info(s"Processing message ${message.getMessageId}")
    val processMessageFuture = for {
      t <- Future.fromTry(read(message))
      _ <- process(t)
    } yield message

    processMessageFuture.failed.foreach {
      case exception: GracefulFailureException =>
        logger.warn(
          s"Graceful failure processing message ${message.getMessageId}: ${exception.getMessage}")
      case exception: Exception =>
        logger.error(
          s"Unrecognised failure while processing message ${message.getMessageId}",
          exception)
        metricsSender.incrementCount(
          metricName = s"${streamName}_MessageProcessingFailure",
          count = 1.0
        )
    }
    processMessageFuture
  }

  private def read(message: sqs.model.Message)(
    implicit decoderT: Decoder[T]): Try[T] =
    fromJson[T](message.getBody)
}
