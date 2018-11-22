package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SQSBuilder
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream

object MessagingBuilder {
  def buildMessageStream[T, R](config: Config): MessageStream[T, R] =
    new MessageStream[T, R](
      actorSystem = AkkaBuilder.buildActorSystem(),
      sqsClient = SQSBuilder.buildSQSAsyncClient(config),
      sqsConfig = SQSBuilder.buildSQSConfig(config),
      metricsSender = MetricsBuilder.buildMetricsSender(config)
    )
}
