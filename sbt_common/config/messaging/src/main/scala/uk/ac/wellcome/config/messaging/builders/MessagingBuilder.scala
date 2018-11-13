package uk.ac.wellcome.config.messaging.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.messaging.message.{
  MessageReaderConfig,
  MessageStream,
  MessageWriter,
  MessageWriterConfig
}
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.storage.type_classes.SerialisationStrategy

import scala.concurrent.ExecutionContext

object MessagingBuilder {
  def buildMessageReaderConfig(config: Config): MessageReaderConfig =
    MessageReaderConfig(
      sqsConfig =
        SQSBuilder.buildSQSConfig(config, namespace = "message.reader"),
      s3Config = S3Builder.buildS3Config(config, namespace = "message.reader")
    )

  def buildMessageStream[T](config: Config)(
    implicit serialisationStrategy: SerialisationStrategy[T])
    : MessageStream[T] = {
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
      s3Client = S3Builder.buildS3Client(config)
    )

    new MessageStream[T](
      actorSystem = AkkaBuilder.buildActorSystem(),
      sqsClient = SQSBuilder.buildSQSAsyncClient(config),
      s3Client = S3Builder.buildS3Client(config),
      messageReaderConfig = buildMessageReaderConfig(config),
      metricsSender = MetricsBuilder.buildMetricsSender(config)
    )
  }

  def buildMessageWriterConfig(config: Config): MessageWriterConfig =
    MessageWriterConfig(
      snsConfig =
        SNSBuilder.buildSNSConfig(config, namespace = "message.reader"),
      s3Config = S3Builder.buildS3Config(config, namespace = "message.reader")
    )

  def buildMessageWriter[T](config: Config)(
    implicit serialisationStrategy: SerialisationStrategy[T])
    : MessageWriter[T] = {
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    implicit val storageBackend: S3StorageBackend = new S3StorageBackend(
      s3Client = S3Builder.buildS3Client(config)
    )

    new MessageWriter[T](
      messageConfig = buildMessageWriterConfig(config),
      snsClient = SNSBuilder.buildSNSClient(config),
      s3Client = S3Builder.buildS3Client(config)
    )
  }
}
