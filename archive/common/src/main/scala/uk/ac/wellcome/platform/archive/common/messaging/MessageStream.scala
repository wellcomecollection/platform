package uk.ac.wellcome.platform.archive.common.messaging

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream._
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckFlow, SqsSource}
import akka.stream.scaladsl.{Flow, Sink}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.flows.RejoinFlow

class MessageStream[T, R] @Inject()(
  sqsClient: AmazonSQSAsync,
  sqsConfig: SQSConfig,
  metricsSender: MetricsSender
)(
  implicit dec: Decoder[T]
) extends Logging {

  def run(name: String, flow: Flow[T, R, NotUsed])(
    implicit
      a: LoggingAdapter,
      m: Materializer
  ) = {

    val source = SqsSource(sqsConfig.queueUrl)(sqsClient)
    val ackFlow = SqsAckFlow(sqsConfig.queueUrl)(sqsClient)
    val sink = Sink.ignore
    val messageFlow = MessageParsingFlow[T]()
    val deleteFlow = Flow[Message]
      .map((_, MessageAction.Delete))
      .via(ackFlow)
    val workflow = RejoinFlow(messageFlow.via(flow))

    source
      .via(workflow)
      .log("Application flow completed.")
      .map { case (m, _) => m }
      .via(deleteFlow)
      .log("Message deleted.")
      .runWith(sink)
  }
}

