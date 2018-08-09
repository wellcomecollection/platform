package uk.ac.wellcome.platform.archiver.messaging

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream._
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{AckResult, SqsAckFlow, SqsSource}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.dynamo.DynamoNonFatalError

import scala.util.{Failure, Success, Try}

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

    val stoppingMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withSupervisionStrategy(decider(metricName, Supervision.Stop)))

    val resumingMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withSupervisionStrategy(decider(metricName, Supervision.Resume)))

    val typeConversion = Flow[Message].map( m => {
      val msg = fromJson[T](m.getBody).get
      info(s"Message received: $msg")
      msg
    })

    val actionFlow = Flow[(Seq[Try[R]], Message)].map {
      case (s, m) if s.collect({ case a: Failure[_] => a }).nonEmpty =>
        metricsSender.countFailure(metricName)
        warn(s"Failure during message processing: $m")
        (m, MessageAction.ChangeMessageVisibility(1))
      case (_, m) =>
        metricsSender.countSuccess(metricName)
        info(s"Message processed successfully: $m")
        (m, MessageAction.Delete)
    }

    val capturedWorkflow: Flow[T, Seq[Try[R]], NotUsed] =
      Flow[T].flatMapConcat(t => {
        Source.fromFuture(
          Source
            .single(t)
            .via(workFlow)
            .map(Success(_))
            .recover({ case e => Failure(e) })
            .toMat(Sink.seq)(Keep.right)
            .run()(stoppingMaterializer)
        )
      })

    source
      .flatMapConcat(message => {
        Source
          .single(message)
          .log("processing message")
          .via(typeConversion)
          .log("message converted")
          .via(capturedWorkflow)
          .log("workflow completed")
          .map(r => (r, message))
          .via(actionFlow)
          .log("message action")
          .via(ackFlow)
          .log("message completed")
      })
      .runWith(sink)(resumingMaterializer)
  }

  // Defines a "supervision strategy" -- this tells Akka how to react
  // if one of the elements fails.  We want to log the failing message,
  // then drop it and carry on processing the next message.
  //
  // https://doc.akka.io/docs/akka/2.5.6/scala/stream/stream-error.html#supervision-strategies
  //
  private def decider(metricName: String,
                      strategy: Supervision.Directive): Supervision.Decider = {
    case e =>
      logException(e)
      metricsSender.countFailure(metricName)
      strategy
  }

  private def logException(exception: Throwable): Unit = {
    exception match {
      case exception: GracefulFailureException =>
        warn(s"Graceful failure: ${exception.getMessage}")
      case exception: DynamoNonFatalError =>
        warn(s"Non-fatal DynamoDB error: ${exception.getMessage}")
      case exception: Exception =>
        error(
          s"Unrecognised failure while: ${exception.getMessage}",
          exception)
    }
  }
}
