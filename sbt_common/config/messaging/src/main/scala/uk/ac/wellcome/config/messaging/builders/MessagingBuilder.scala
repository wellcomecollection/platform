package uk.ac.wellcome.config.messaging.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.messaging.message.{MessageReaderConfig, MessageStream}

object MessagingBuilder {
  def buildMessageReaderConfig(config: Config): MessageReaderConfig =
    MessageReaderConfig(
      sqsConfig = SQSBuilder.buildSQSConfig(config, namespace = "message.reader"),
      s3Config = S3Builder.buildS3Config(config, namespace = "message.reader")

    )

  def buildMessageStream[T](config: Config): MessageStream[T] =
    new MessageStream[T](
      actorSystem = AkkaBuilder.buildActorSystem(),
      sqsClient = SQSBuilder.buildSQSAsyncClient(config),
      s3Client = S3Builder.buildS3Client(config),
      messageReaderConfig = buildMessageReaderConfig(config),
      metricsSender = MetricsBuilder.buildMetricsSender(config)
    )
}
