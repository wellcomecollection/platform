package uk.ac.wellcome.platform.archiver.messaging

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream._
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{AckResult, SqsAckFlow, SqsSource}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.dynamo.DynamoNonFatalError

class MessageStream[T, R] @Inject()(actorSystem: ActorSystem,
                                    sqsClient: AmazonSQSAsync,
                                    sqsConfig: SQSConfig,
                                    metricsSender: MetricsSender)
  extends Logging {

  implicit val system = actorSystem
  implicit val dispatcher = system.dispatcher

  private val source = SqsSource(sqsConfig.queueUrl)(sqsClient)
  private val ackFlow: Flow[(Message, MessageAction), AckResult, NotUsed] =
    SqsAckFlow(sqsConfig.queueUrl)(sqsClient)
  private val sink = Sink.ignore

  def run(streamName: String, workFlow: Flow[T, R, NotUsed])(
    implicit decoderT: Decoder[T],
    loggingAdapter: LoggingAdapter
  ) = {

    val metricName = s"${streamName}_ProcessMessage"

    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withSupervisionStrategy(decider(metricName)))

    val typeConversion = Flow[Message].map(m => fromJson[T](m.getBody).get)
    val actionFlow = Flow[(R, Message)].map {
      case (_, m) =>
        metricsSender.countSuccess(metricName)
        (m, MessageAction.Delete)
    }

//    val myFlow = Flow[T].flatMapConcat(_ =>
//      Source(1 to 4).map(i =>
//        throw new RuntimeException("oh no!")
//
////    if(i < 2) {
////          i
////        } else {
////          throw new RuntimeException("oh no!")
////        }
//      )
//    )

    source.flatMapConcat(message => {
      Source.single(message)
        .log("processing message")
        .via(typeConversion)
        .log("message converted")
        .via(workFlow)
        .log("workflow completed")
        .map(r => (r, message))
        .via(actionFlow)
        .log("message action")
        .via(ackFlow)
        .log("message completed")
    }).runWith(sink)
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
      Supervision.resume
    case _ => Supervision.Stop
  }

  private def logException(exception: Throwable): Unit = {
    exception match {
      case exception: GracefulFailureException =>
        logger.warn(s"Graceful failure: ${exception.getMessage}")
      case exception: DynamoNonFatalError =>
        logger.warn(s"Non-fatal DynamoDB error: ${exception.getMessage}")
      case exception: Exception =>
        logger.error(
          s"Unrecognised failure while: ${exception.getMessage}",
          exception)
    }
  }
}
