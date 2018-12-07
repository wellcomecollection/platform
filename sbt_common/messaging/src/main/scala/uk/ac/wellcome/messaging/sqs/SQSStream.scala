package uk.ac.wellcome.messaging.sqs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.MessageAction.Delete
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Keep, Source}
import akka.{Done, NotUsed}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.dynamo.DynamoNonFatalError

import scala.concurrent.Future

// Provides a stream for processing SQS messages.
//
// The main entry point is `foreach` -- callers should create an instance of
// this class, then pass the name of the stream and a processing function
// to foreach.  For example:
//
//      val sqsStream = SQSStream[NotificationMessage]
//
//      def processMessage(message: NotificationMessage): Future[Unit]
//
//      sqsStream.foreach(
//        streamName = "ExampleStream",
//        process = processMessage
//      )
//
class SQSStream[T](
  sqsClient: AmazonSQSAsync,
  sqsConfig: SQSConfig,
  metricsSender: MetricsSender)(implicit val actorSystem: ActorSystem)
    extends Logging {

  implicit val dispatcher = actorSystem.dispatcher

  private val source = SqsSource(sqsConfig.queueUrl)(sqsClient)
  private val sink = SqsAckSink(sqsConfig.queueUrl)(sqsClient)

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderT: Decoder[T]): Future[Done] =
    runStream(
      streamName = streamName,
      source =>
        source
          .mapAsyncUnordered(parallelism = sqsConfig.parallelism) {
            case (message, t) =>
              debug(s"Processing message ${message.getMessageId}")
              process(t).map(_ => message)
        }
    )

  def runStream(
    streamName: String,
    modifySource: Source[(Message, T), NotUsed] => Source[Message, NotUsed])(
    implicit decoder: Decoder[T]): Future[Done] = {
    val metricName = s"${streamName}_ProcessMessage"

    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(decider(metricName)))

    val src: Source[Message, NotUsed] = modifySource(source.map { message =>
      (message, fromJson[T](message.getBody).get)
    })

    val srcWithLogging: Source[(Message, Delete.type), NotUsed] = src
      .map { m =>
        metricsSender.countSuccess(metricName)
        debug(s"Deleting message ${m.getMessageId}")
        (m, MessageAction.Delete)
      }

    srcWithLogging
      .toMat(sink)(Keep.right)
      .run()
  }

  // Defines a "supervision strategy" -- this tells Akka how to react
  // if one of the elements fails.  We want to log the failing message,
  // then drop it and carry on processing the next message.
  //
  // https://doc.akka.io/docs/akka/2.5.6/scala/stream/stream-error.html#supervision-strategies
  //
  private def decider(metricName: String): Supervision.Decider = {
    case e @ (_: GracefulFailureException | _: JsonDecodingError) =>
      logException(e)
      metricsSender.countRecognisedFailure(metricName)
      Supervision.resume
    case e: Exception =>
      logException(e)
      metricsSender.countFailure(metricName)
      Supervision.Resume
    case _ => Supervision.Stop
  }

  private def logException(exception: Throwable): Unit = {
    exception match {
      case exception: GracefulFailureException =>
        logger.warn(s"Graceful failure: ${exception.getMessage}")
      case exception: DynamoNonFatalError =>
        logger.warn(s"Non-fatal DynamoDB error: ${exception.getMessage}")
      case exception: JsonDecodingError =>
        logger.warn(s"JSON decoding error: ${exception.getMessage}")
      case exception: Exception =>
        logger.error(
          s"Unrecognised failure while: ${exception.getMessage}",
          exception)
    }
  }
}
