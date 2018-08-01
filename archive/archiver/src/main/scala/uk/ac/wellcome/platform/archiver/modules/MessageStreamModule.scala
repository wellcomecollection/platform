package uk.ac.wellcome.platform.archiver.modules

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archiver.MessageStream


object MessageStreamModule extends AbstractModule {
  @Provides
  def providesMessageStream(
                             actorSystem: ActorSystem,
                             sqsClient: AmazonSQSAsync,
                             sqsConfig: SQSConfig,
                             metricsSender: MetricsSender) = {
    new MessageStream[NotificationMessage, Unit](
      actorSystem, sqsClient, sqsConfig, metricsSender
    )
  }
}