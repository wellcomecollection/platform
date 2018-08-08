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


class MessageStream[T, R] @Inject()(actorSystem: ActorSystem,
                                    sqsClient: AmazonSQSAsync,
                                    sqsConfig: SQSConfig,
                                    metricsSender: MetricsSender)
  extends Logging {

  implicit val system = actorSystem
  implicit val dispatcher = system.dispatcher

  system.registerOnTermination(sqsClient.shutdown())

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
    val actionFlow = Flow[(Seq[T], Message)].map {
      case (Seq(), m) =>
        (m, MessageAction.Ignore)
      case (_, m) =>
        metricsSender.countSuccess(metricName)
        (m, MessageAction.Delete)
    }

    //    val myFlow = Flow[T].flatMapConcat(t => {
    //      val futureSeq = Source.fromFuture(
    //        Source.single(t)
    //          .map(o => {
    //            throw new RuntimeException("BLUGBLUG")
    //
    //            o
    //          })
    //          .map(Success(_))
    //          .recover {
    //            case e: RuntimeException => Failure(e)
    //          }
    //          .toMat(Sink.seq)(Keep.right)
    //          .run()
    //      )
    //
    //      futureSeq
    //    })

    val myFlow: Flow[T, Seq[T], NotUsed] = Flow[T].flatMapConcat(t => {
      Source.fromFuture(
        Source.single(t)
          .map(o => {
            throw new RuntimeException("BLUGBLUG")

            o
          })
          .toMat(Sink.seq)(Keep.right)
          .run()
      )
    })

//    val capturedWorkFlow: Flow[T, Seq[R], NotUsed] = Flow[T].flatMapConcat(t => {
//      Source.fromFuture(
//        Source.single(t)
//          .via(workFlow)
//          .toMat(Sink.seq)(Keep.right)
//          .run()
//      )
//    })

    val messageMonitor = Flow[Message].map(msg => {
      println(s"messageMonitor: $msg")
    }).to(Sink.ignore)

    source.alsoTo(messageMonitor).flatMapConcat(message => {
      Source.single(message)
        .log("processing message")
        .via(typeConversion)
        .log("message converted")
        .via(myFlow)
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
    case e =>
      logException(e)
      metricsSender.countFailure(metricName)
      Supervision.Resume
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
