package uk.ac.wellcome.platform.archive.notifier

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.PublishResult
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.notifier.flows.NotificationFlow

import uk.ac.wellcome.json.JsonUtil._

class Notifier @Inject()(
                          sqsClient: AmazonSQSAsync,
                          sqsConfig: SQSConfig,
                          snsClient: AmazonSNSAsync,
                          snsConfig: SNSConfig,
                          actorSystem: ActorSystem,
                          metricsSender: MetricsSender
                        ) {
  def run() = {
    implicit val system = actorSystem

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val stream =
      new MessageStream[Progress, PublishResult](
        sqsClient, sqsConfig, metricsSender)

    val workFlow = NotificationFlow(snsClient, snsConfig)

    stream.run("notifier", workFlow)
  }
}
