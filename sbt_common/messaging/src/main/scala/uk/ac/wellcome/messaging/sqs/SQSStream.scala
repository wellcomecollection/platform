package uk.ac.wellcome.messaging.sqs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Keep, Source}
import akka.{Done, NotUsed}
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

  def decider(metricName: String): Supervision.Decider = {
    case e: Exception =>
      metricsSender.count(metricName, Future.failed(e))
      Supervision.Resume
    case _ => Supervision.Stop
  }

  implicit val system = actorSystem

  implicit val dispatcher = system.dispatcher

  private val source = SqsSource(sqsConfig.queueUrl)(sqsClient)
  private val sink = SqsAckSink(sqsConfig.queueUrl)(sqsClient)

  def runStream[M](streamName: String, modifySource: Source[(Message,T),NotUsed] => Source[Message,M])(implicit decoder: Decoder[T]): Future[Done] = {
    val metricName = s"${streamName}_ProcessMessage"

    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system).withSupervisionStrategy(decider(metricName)))

    modifySource(source.map(message => (message, read(message).get)))
      .map { m =>
        metricsSender.count(metricName, Future.successful(()))
        debug(s"Deleting message ${m.getMessageId}")
        (m, MessageAction.Delete)
      }
      .toMat(sink)(Keep.right).run()
  }

  def foreach(streamName: String, process: T => Future[Unit])(
    implicit decoderT: Decoder[T]): Future[Done] =
    runStream(s"$streamName",
      _.mapAsyncUnordered(parallelism = sqsConfig.parallelism) { case (message, t) =>
        debug(s"Processing message ${message.getMessageId}")
        readAndProcess(streamName, t, process).map(_ => message)
      })

  private def readAndProcess(
    streamName: String,
    message: T,
    process: T => Future[Unit]) = {
    val processMessageFuture = process(message)

    processMessageFuture.failed.foreach {
      case exception: GracefulFailureException =>
        logger.warn(
          s"Graceful failure processing message: ${exception.getMessage}")
      case exception: Exception =>
        logger.error(
          s"Unrecognised failure while processing message: ${exception.getMessage}",
          exception)
    }
    processMessageFuture
  }

  private def read(message: sqs.model.Message)(
    implicit decoderT: Decoder[T]): Try[T] =
    fromJson[T](message.getBody)
}
