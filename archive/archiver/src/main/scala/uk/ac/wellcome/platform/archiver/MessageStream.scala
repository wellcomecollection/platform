package uk.ac.wellcome.platform.archiver

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Zip}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, Supervision}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.storage.dynamo.DynamoNonFatalError
import uk.ac.wellcome.utils.JsonUtil.fromJson

import scala.concurrent.Future


class MessageStream[T, R] @Inject()(actorSystem: ActorSystem,
                                    sqsClient: AmazonSQSAsync,
                                    sqsConfig: SQSConfig,
                                    metricsSender: MetricsSender)
  extends Logging {

  implicit val system = actorSystem
  implicit val dispatcher = system.dispatcher

  private val source = SqsSource(sqsConfig.queueUrl)(sqsClient)
  private val sink = SqsAckSink(sqsConfig.queueUrl)(sqsClient)

  def run(streamName: String, workFlow: Flow[T, R, NotUsed])(implicit decoderT: Decoder[T]) = {
    val metricName = s"${streamName}_ProcessMessage"

    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withSupervisionStrategy(decider(metricName)))

    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val in = source
      val out = sink

      val typeConversion = Flow[Message].map(m => fromJson[T](m.getBody).get)

      val messageAckFlow = Flow[(R, Message)].map { case (r, m) =>
        metricsSender.count(metricName, Future.successful(()))
        debug(s"Deleting message ${m.getMessageId}")

        (m, MessageAction.Delete)
      }

      val messageRecieptLogFlow = Flow[Message].map(m => {
        debug(s"Processing message ${m.getMessageId}")

        m
      })

      val broadcast = builder.add(Broadcast[Message](2))
      val zip = builder.add(Zip[R, Message])

      in ~> messageRecieptLogFlow ~> broadcast ~> typeConversion ~> workFlow ~> zip.in0
      broadcast ~> zip.in1

      zip.out ~> messageAckFlow ~> out

      ClosedShape
    }).run()
  }

  private def decider(metricName: String): Supervision.Decider = {
    case e: Exception =>
      logException(e)
      metricsSender.count(metricName, Future.failed(e))
      Supervision.Resume
    case _ => Supervision.Stop
  }

  private def logException(exception: Exception) = {
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
