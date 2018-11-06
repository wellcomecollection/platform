package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.messaging.{
  MessageStream,
  NotificationParsingFlow
}
import uk.ac.wellcome.platform.archive.common.models.{
  CallbackNotification,
  NotificationMessage
}
import uk.ac.wellcome.platform.archive.notifier.flows.NotificationFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification._

import scala.concurrent.Future

class Notifier @Inject()(
  sqsClient: AmazonSQSAsync,
  sqsConfig: SQSConfig,
  snsClient: AmazonSNS,
  snsConfig: SNSConfig,
  metricsSender: MetricsSender,
  contextUrl: URL
)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {
  def run(): Future[Done] = {

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val stream =
      new MessageStream[NotificationMessage, PublishResult](
        actorSystem,
        sqsClient,
        sqsConfig,
        metricsSender)

    val notificationParsingFlow =
      NotificationParsingFlow[CallbackNotification]()

    val workflow = NotificationFlow(contextUrl, snsClient, snsConfig)

    val flow = notificationParsingFlow.via(workflow)

    stream.run("notifier", flow)
  }
}
