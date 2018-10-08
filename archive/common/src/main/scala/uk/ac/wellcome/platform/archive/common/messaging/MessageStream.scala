package uk.ac.wellcome.platform.archive.common.messaging

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream._
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{AckResult, SqsAckFlow, SqsSource}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender

import scala.concurrent.Future

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
    loggingAdapter: LoggingAdapter,
    materializer: Materializer
  ): Future[Done] = {

    val typeConversion = Flow[Message].map(m => {
      val msg = fromJson[T](m.getBody).get
      info(s"Message received: $msg")
      msg
    })

    source
      .flatMapMerge(
        sqsConfig.parallelism,
        (message: Message) => {
          Source
            .single(message)
            .log("processing message")
            .via(typeConversion)
            .log("message converted")
            .via(workFlow)
            .log("workflow completed")
            .map(_ => (message, MessageAction.Delete))
            .log("message action")
            .via(ackFlow)
            .log("message completed")
        }
      )
      .runWith(sink)
  }
}
